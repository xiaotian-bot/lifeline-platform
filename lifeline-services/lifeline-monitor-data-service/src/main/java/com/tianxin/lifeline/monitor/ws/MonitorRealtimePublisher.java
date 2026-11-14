package com.tianxin.lifeline.monitor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.monitor.dto.SensorRawMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 实时监测数据 Redis Pub/Sub 发布端（写入侧扇出源）。
 *
 * <p>实时数据落 Redis（{@code rt:{deviceId}}）处调用本组件，向频道
 * {@code monitor:rt:{areaId}} 发布 JSON 消息（deviceId + 核心指标）；
 * 各服务副本的订阅端收到后向对应区域 WebSocket 会话广播。
 *
 * <p><b>主链路隔离</b>：发布失败仅记录 warn 日志，绝不影响
 * TDengine/Redis/PG 写入主链路；推送开关关闭或 areaId 缺失时静默跳过。
 */
@Slf4j
@Component
public class MonitorRealtimePublisher {

    /** 频道前缀：monitor:rt:{areaId} */
    public static final String CHANNEL_PREFIX = "monitor:rt:";

    /** Redis 操作模板（convertAndSend 发布） */
    private final StringRedisTemplate stringRedisTemplate;

    /** WebSocket 配置属性（enabled 开关） */
    private final MonitorWebSocketProperties properties;

    /** JSON 序列化器（消息体为 SensorRawMessage 全字段） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造发布端。
     *
     * @param stringRedisTemplate Redis 操作模板
     * @param properties          WebSocket 配置属性
     */
    public MonitorRealtimePublisher(StringRedisTemplate stringRedisTemplate,
                                    MonitorWebSocketProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 发布实时监测数据到区域频道（失败仅日志，不抛出）。
     *
     * @param message 已完成归一化的传感数据
     */
    public void publish(SensorRawMessage message) {
        if (!properties.isEnabled()) {
            return;
        }
        String areaId = message.getAreaId();
        if (areaId == null || areaId.isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + areaId.trim(), json);
        } catch (Exception e) {
            log.warn("实时推送消息发布失败(不影响写入主链路): deviceId={}, areaId={}, cause={}",
                    message.getDeviceId(), areaId, e.getMessage());
        }
    }
}
