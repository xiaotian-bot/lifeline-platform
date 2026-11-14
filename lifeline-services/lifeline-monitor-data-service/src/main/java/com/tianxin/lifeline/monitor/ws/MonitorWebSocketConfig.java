package com.tianxin.lifeline.monitor.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 端点注册配置（P10-3）。
 *
 * <p>注册实时监测推送端点 {@code /ws/monitor/{areaId}}（模式
 * {@code /ws/monitor/*} 单级路径变量）。网关已配置
 * {@code lb:ws://monitor-data-service} 路由与 {@code /ws/**} 免鉴权白名单，
 * 前端经网关以 {@code ws://host/ws/monitor/AREA-001} 接入。
 *
 * <p>与现有 MVC 栈共存：Spring WebSocket（Servlet 容器握手）不引入
 * STOMP，消息面最小化，仅服务端推送 + 客户端心跳。
 */
@Configuration
@EnableWebSocket
public class MonitorWebSocketConfig implements WebSocketConfigurer {

    /** 实时推送处理器 */
    private final MonitorWebSocketHandler monitorWebSocketHandler;

    /** 握手鉴权拦截器 */
    private final MonitorWebSocketHandshakeInterceptor handshakeInterceptor;

    /**
     * 构造配置。
     *
     * @param monitorWebSocketHandler 实时推送处理器
     * @param handshakeInterceptor    握手鉴权拦截器
     */
    public MonitorWebSocketConfig(MonitorWebSocketHandler monitorWebSocketHandler,
                                  MonitorWebSocketHandshakeInterceptor handshakeInterceptor) {
        this.monitorWebSocketHandler = monitorWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    /**
     * 注册 WebSocket 端点。
     *
     * @param registry 处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(monitorWebSocketHandler, "/ws/monitor/*")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
