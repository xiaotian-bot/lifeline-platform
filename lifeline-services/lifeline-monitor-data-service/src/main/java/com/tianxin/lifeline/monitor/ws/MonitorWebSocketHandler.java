package com.tianxin.lifeline.monitor.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

/**
 * 监测实时推送 WebSocket 处理器（端点 {@code /ws/monitor/{areaId}}）。
 *
 * <p>按 URI 末段解析区域编码并注册会话；连接数超上限（单区域/全局）
 * 时拒绝新连接；连接关闭与传输异常时自动清理会话。客户端上行文本仅
 * 作心跳处理（回复 pong），不做业务解析。
 */
@Slf4j
@Component
public class MonitorWebSocketHandler extends TextWebSocketHandler {

    /** 端点路径前缀（URI 形如 /ws/monitor/AREA-001） */
    private static final String PATH_PREFIX = "/ws/monitor/";

    /** 会话注册表 */
    private final MonitorWebSocketSessionRegistry registry;

    /** WebSocket 配置属性（连接数上限 / 启用开关） */
    private final MonitorWebSocketProperties properties;

    /** 运行指标收集器 */
    private final MonitorWebSocketMetrics metrics;

    /**
     * 构造处理器。
     *
     * @param registry   会话注册表
     * @param properties WebSocket 配置属性
     * @param metrics    运行指标收集器
     */
    public MonitorWebSocketHandler(MonitorWebSocketSessionRegistry registry,
                                   MonitorWebSocketProperties properties,
                                   MonitorWebSocketMetrics metrics) {
        this.registry = registry;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 连接建立：解析 areaId、连接数保护校验、注册会话。
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!properties.isEnabled()) {
            session.close(CloseStatus.SERVICE_RESTARTED.withReason("实时推送已停用"));
            return;
        }
        String areaId = extractAreaId(session);
        if (areaId == null || areaId.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("缺少区域编码: /ws/monitor/{areaId}"));
            return;
        }
        // 连接数上限保护：全局与单区域双重校验
        if (registry.countTotal() >= properties.getMaxTotalSessions()) {
            log.warn("WebSocket 连接拒绝(全局会话数达上限 {}): areaId={}",
                    properties.getMaxTotalSessions(), areaId);
            session.close(CloseStatus.SERVICE_OVERLOAD.withReason("连接数已达服务上限"));
            return;
        }
        if (registry.countByArea(areaId) >= properties.getMaxSessionsPerArea()) {
            log.warn("WebSocket 连接拒绝(区域会话数达上限 {}): areaId={}",
                    properties.getMaxSessionsPerArea(), areaId);
            session.close(CloseStatus.SERVICE_OVERLOAD.withReason("该区域连接数已达上限"));
            return;
        }
        session.getAttributes().put("areaId", areaId);
        registry.register(areaId, session);
        metrics.recordConnection();
        log.info("WebSocket 连接建立: areaId={}, sessionId={}, userId={}, 区域会话数={}, 全局会话数={}",
                areaId, session.getId(), session.getAttributes().get("userId"),
                registry.countByArea(areaId), registry.countTotal());
    }

    /**
     * 客户端上行文本：视为心跳，回复 pong（不做业务解析）。
     *
     * @param session WebSocket 会话
     * @param message 上行文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("pong"));
            metrics.recordHeartbeat();
        }
    }

    /**
     * 连接关闭：清理会话（心跳超时/客户端断开均由容器回调至此）。
     *
     * @param session WebSocket 会话
     * @param status  关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanup(session);
        metrics.recordDisconnection();
        log.debug("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 传输异常：清理会话（异常本身不向客户端抛出，避免级联失败）。
     *
     * @param session   WebSocket 会话
     * @param exception 传输异常
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        cleanup(session);
        log.warn("WebSocket 传输异常, 会话已清理: sessionId={}, cause={}",
                session.getId(), exception.getMessage());
    }

    /**
     * 清理会话（从注册表移除并尽力关闭连接）。
     *
     * @param session WebSocket 会话
     */
    private void cleanup(WebSocketSession session) {
        Object areaId = session.getAttributes().get("areaId");
        if (areaId instanceof String area) {
            registry.remove(area, session);
        }
        if (session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                log.debug("关闭 WebSocket 会话异常(忽略): sessionId={}", session.getId());
            }
        }
    }

    /**
     * 从会话 URI 解析区域编码（末段路径）。
     *
     * @param session WebSocket 会话
     * @return 区域编码，解析失败返回 null
     */
    private String extractAreaId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || !path.startsWith(PATH_PREFIX)) {
            return null;
        }
        String areaId = path.substring(PATH_PREFIX.length());
        // 去除可能的尾部斜杠与查询参数残留
        int slash = areaId.indexOf('/');
        if (slash >= 0) {
            areaId = areaId.substring(0, slash);
        }
        return areaId.trim();
    }
}
