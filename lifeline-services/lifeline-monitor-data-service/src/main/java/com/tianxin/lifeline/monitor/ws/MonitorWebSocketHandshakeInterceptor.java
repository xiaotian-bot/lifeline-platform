package com.tianxin.lifeline.monitor.ws;

import com.tianxin.lifeline.common.security.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手前置拦截器（P10-3 安全加固）。
 *
 * <p>在 HTTP→WebSocket 升级握手前校验 JWT（从查询参数 {@code token} 或
 * {@code Authorization} 请求头提取），校验通过后将 userId / username / roles
 * 写入 WebSocket 会话属性，供后续处理器使用。
 *
 * <p>校验失败直接拒绝握手（返回 false），不建立 WebSocket 连接。
 * 网关虽已将 {@code /ws/**} 加入白名单（WebSocket 升级请求无法携带自定义头），
 * 鉴权责任下沉至本拦截器。
 */
@Slf4j
@Component
public class MonitorWebSocketHandshakeInterceptor
        implements HandshakeInterceptor {

    /** 查询参数名：前端通过 ws://host/ws/monitor/{areaId}?token=xxx 传入 */
    private static final String TOKEN_PARAM = "token";

    /** Authorization 请求头 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 会话属性键 */
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_ROLES = "roles";

    /** JWT 配置属性 */
    private final SecurityProperties securityProperties;

    /** WebSocket 指标收集器 */
    private final MonitorWebSocketMetrics metrics;

    /**
     * 构造握手拦截器。
     *
     * @param securityProperties JWT 配置属性
     * @param metrics            指标收集器
     */
    public MonitorWebSocketHandshakeInterceptor(SecurityProperties securityProperties,
                                                 MonitorWebSocketMetrics metrics) {
        this.securityProperties = securityProperties;
        this.metrics = metrics;
    }

    /**
     * 握手前校验 JWT。
     *
     * @param request    HTTP 升级请求
     * @param response   HTTP 响应
     * @param wsHandler  WebSocket 处理器
     * @param attributes 会话属性（可写入，传递给 WebSocketSession）
     * @return true 放行握手 / false 拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            log.warn("WebSocket 握手拒绝(缺少token): path={}", request.getURI().getPath());
            metrics.recordAuthFailure();
            return false;
        }

        try {
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            String username = claims.get("username", String.class);

            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_USERNAME, username != null ? username : "");
            // roles 在 claim 中为 List<String>，直接存入会话属性
            Object roles = claims.get("roles");
            attributes.put(ATTR_ROLES, roles);

            metrics.recordAuthSuccess();
            log.debug("WebSocket 握手鉴权通过: userId={}, username={}", userId, username);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("WebSocket 握手拒绝(token无效): path={}, reason={}",
                    request.getURI().getPath(), e.getMessage());
            metrics.recordAuthFailure();
            return false;
        }
    }

    /**
     * 握手完成后回调（无论成功失败均触发），此处仅做 debug 日志。
     *
     * @param request   HTTP 升级请求
     * @param response  HTTP 响应
     * @param wsHandler WebSocket 处理器
     * @param exception 握手异常（无异常时为 null）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.warn("WebSocket 握手异常: path={}, reason={}",
                    request.getURI().getPath(), exception.getMessage());
        }
    }

    /**
     * 从请求中提取 JWT token（优先查询参数，其次 Authorization 头）。
     *
     * @param request HTTP 请求
     * @return 纯 token 字符串，无 token 返回 null
     */
    private String resolveToken(ServerHttpRequest request) {
        // 1. 查询参数 ?token=xxx（WebSocket 升级请求最常用的传参方式）
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2 && TOKEN_PARAM.equals(kv[0])) {
                    return kv[1].trim();
                }
            }
        }
        // 2. Authorization 请求头（部分客户端可能在升级请求中携带）
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String authorization = servletRequest.getServletRequest().getHeader(AUTHORIZATION_HEADER);
            if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
                return authorization.substring(BEARER_PREFIX.length()).trim();
            }
        }
        return null;
    }

    /**
     * 解析并校验 JWT，返回 Claims。
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims
     * @throws JwtException 校验失败
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(securityProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
