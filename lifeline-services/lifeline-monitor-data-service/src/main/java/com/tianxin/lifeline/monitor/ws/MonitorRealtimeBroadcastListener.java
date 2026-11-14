package com.tianxin.lifeline.monitor.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时监测数据 Redis Pub/Sub 订阅广播监听器。
 *
 * <p>以模式 {@code monitor:rt:*} 订阅全部区域频道（服务无状态：
 * 多副本各自订阅，Redis 向所有订阅者投递，天然水平扩展），解析频道
 * 后缀 areaId 后向对应区域会话广播原始 JSON。
 *
 * <p><b>广播节流</b>：同一 areaId + deviceId 组合在
 * {@code lifeline.monitor.ws.throttle-per-device-ms}（默认 1 秒）内
 * 最多广播一条，防止高频上报打爆前端连接。节流窗口为本地副本粒度，
 * 会话仅挂在单一副本上，语义等价于全局每设备限频。
 */
@Slf4j
@Component
public class MonitorRealtimeBroadcastListener implements MessageListener {

    /** 会话注册表 */
    private final MonitorWebSocketSessionRegistry registry;

    /** WebSocket 配置属性（节流间隔） */
    private final MonitorWebSocketProperties properties;

    /** JSON 解析器（仅提取 deviceId 用于节流键） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 运行指标收集器 */
    private final MonitorWebSocketMetrics metrics;

    /** 节流窗口记录：areaId:deviceId → 最近一次广播时间戳（毫秒） */
    private final ConcurrentHashMap<String, Long> lastBroadcastAt = new ConcurrentHashMap<>();

    /**
     * 构造广播监听器。
     *
     * @param registry   会话注册表
     * @param properties WebSocket 配置属性
     * @param metrics    运行指标收集器
     */
    public MonitorRealtimeBroadcastListener(MonitorWebSocketSessionRegistry registry,
                                            MonitorWebSocketProperties properties,
                                            MonitorWebSocketMetrics metrics) {
        this.registry = registry;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * Redis 频道消息回调：解析 areaId → 节流判定 → 区域会话广播。
     *
     * @param message Redis 消息（channel + body）
     * @param pattern 匹配的模式
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            if (!channel.startsWith(MonitorRealtimePublisher.CHANNEL_PREFIX)) {
                return;
            }
            String areaId = channel.substring(MonitorRealtimePublisher.CHANNEL_PREFIX.length());
            Set<WebSocketSession> sessions = registry.getSessions(areaId);
            if (sessions.isEmpty()) {
                // 无订阅会话：跳过解析与广播（节流窗口也无需更新）
                return;
            }
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            if (!passThrottle(areaId, body)) {
                return;
            }
            TextMessage textMessage = new TextMessage(body);
            for (WebSocketSession session : sessions) {
                sendQuietly(session, textMessage, areaId);
                metrics.recordMessagePushed();
            }
        } catch (Exception e) {
            log.warn("实时推送广播异常(已忽略): cause={}", e.getMessage());
        }
    }

    /**
     * 节流判定：同一 areaId + deviceId 在节流窗口内仅放行一条。
     *
     * @param areaId 区域编码
     * @param body   消息 JSON（提取 deviceId 作节流键）
     * @return true 放行 / false 节流丢弃
     */
    private boolean passThrottle(String areaId, String body) {
        long interval = properties.getThrottlePerDeviceMs();
        if (interval <= 0) {
            return true;
        }
        String deviceId = extractDeviceId(body);
        String throttleKey = areaId + ':' + (deviceId == null ? "-" : deviceId);
        long now = System.currentTimeMillis();
        Long last = lastBroadcastAt.get(throttleKey);
        if (last != null && now - last < interval) {
            return false;
        }
        lastBroadcastAt.put(throttleKey, now);
        return true;
    }

    /**
     * 从消息 JSON 提取 deviceId（解析失败返回 null，不阻断广播）。
     *
     * @param body 消息 JSON
     * @return deviceId
     */
    private String extractDeviceId(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            JsonNode deviceIdNode = node.get("deviceId");
            return deviceIdNode == null || deviceIdNode.isNull() ? null : deviceIdNode.asText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 向单个会话静默发送（发送失败/连接已关仅清理会话，不抛出）。
     *
     * @param session   WebSocket 会话
     * @param textMessage 文本消息
     * @param areaId    区域编码（清理用）
     */
    private void sendQuietly(WebSocketSession session, TextMessage textMessage, String areaId) {
        if (!session.isOpen()) {
            registry.remove(areaId, session);
            return;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            log.debug("WebSocket 推送失败, 会话已清理: sessionId={}, cause={}",
                    session.getId(), e.getMessage());
            registry.remove(areaId, session);
        }
    }
}
