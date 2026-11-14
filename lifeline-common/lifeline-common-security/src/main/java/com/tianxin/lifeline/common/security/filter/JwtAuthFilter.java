package com.tianxin.lifeline.common.security.filter;

import cn.hutool.core.util.StrUtil;
import com.tianxin.lifeline.common.security.config.SecurityProperties;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import com.tianxin.lifeline.common.security.dto.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>从请求头 Authorization 中提取 Bearer Token，使用 jjwt 校验签名与有效期，
 * 解析出 userId / username / roles / permissions 构建 {@link UserPrincipal}
 * 写入 {@link SecurityContext}；token 缺失或非法时不阻断请求（鉴权拦截由
 * 网关与各服务自身权限校验负责），请求结束后清理上下文。
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Authorization 请求头 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer 前缀 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 用户名 claim 键 */
    private static final String CLAIM_USERNAME = "username";

    /** JWT 角色 claim 键 */
    private static final String CLAIM_ROLES = "roles";

    /** JWT 权限 claim 键 */
    private static final String CLAIM_PERMISSIONS = "permissions";

    /** JWT 配置属性 */
    private final SecurityProperties securityProperties;

    /**
     * 构造过滤器。
     *
     * @param securityProperties JWT 配置属性
     */
    public JwtAuthFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * 执行 JWT 解析并填充安全上下文。
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StrUtil.isNotBlank(token)) {
                UserPrincipal principal = parseToken(token);
                if (principal != null) {
                    SecurityContext.set(principal);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    /**
     * 从请求头提取 Bearer Token。
     *
     * @param request 当前请求
     * @return 纯token字符串，无token返回 null
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StrUtil.isBlank(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * 解析并校验 JWT，构建用户主体。
     *
     * @param token JWT 字符串
     * @return 用户主体，解析失败返回 null
     */
    @SuppressWarnings("unchecked")
    private UserPrincipal parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(securityProperties.getSecret().getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            List<String> roles = claims.get(CLAIM_ROLES, List.class);
            List<String> permissions = claims.get(CLAIM_PERMISSIONS, List.class);
            return UserPrincipal.builder()
                    .userId(userId)
                    .username(username)
                    .roles(roles == null ? new HashSet<>() : new HashSet<>(roles))
                    .permissions(permissions == null ? new HashSet<>() : new HashSet<>(permissions))
                    .build();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
