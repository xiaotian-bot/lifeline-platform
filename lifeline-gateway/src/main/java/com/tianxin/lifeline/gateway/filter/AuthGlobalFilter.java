package com.tianxin.lifeline.gateway.filter;

import com.tianxin.lifeline.gateway.config.RouteConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 全局鉴权前置过滤器。
 *
 * <p>拦截所有进入网关的请求：白名单路径直接放行；其余请求校验
 * Authorization 头中的 Bearer JWT（签名+有效期），校验失败返回 401 JSON；
 * 校验通过后将 userId 通过请求头透传给下游服务。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** Authorization 请求头 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 透传用户ID的请求头 */
    private static final String USER_ID_HEADER = "X-User-Id";

    /** JWT 签名密钥 */
    @Value("${lifeline.security.jwt.secret:lifeline-platform-default-secret-key-must-be-at-least-256-bits-long}")
    private String secret;

    /** Ant 风格路径匹配器 */
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 过滤器执行逻辑。
     *
     * @param exchange 请求交换对象
     * @param chain    过滤器链
     * @return 响应式处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 提取并校验 token
        String authorization = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "未登录或令牌缺失");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        Claims claims;
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("网关鉴权失败: path={}, reason={}", path, e.getMessage());
            return unauthorized(exchange, "令牌无效或已过期");
        }

        // 透传用户ID给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, claims.getSubject())
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 判断路径是否在免鉴权白名单中。
     *
     * @param path 请求路径
     * @return true 表示放行
     */
    private boolean isWhitelisted(String path) {
        return RouteConfig.AUTH_WHITELIST.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回 401 未认证 JSON 响应。
     *
     * @param exchange 请求交换对象
     * @param message  错误描述
     * @return 响应式完成信号
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":20000,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":"
                + System.currentTimeMillis() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器优先级（最高优先级执行鉴权）。
     *
     * @return 排序值
     */
    @Override
    public int getOrder() {
        return -100;
    }
}
