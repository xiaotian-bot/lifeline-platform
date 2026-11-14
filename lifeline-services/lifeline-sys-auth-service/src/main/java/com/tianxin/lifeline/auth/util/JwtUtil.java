package com.tianxin.lifeline.auth.util;

import com.tianxin.lifeline.auth.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类（jjwt 0.12.x API）。
 *
 * <p>负责签发与解析 Access Token / Refresh Token：
 * <ul>
 *     <li>Access Token：携带 userId、username、roles、permissions，默认 2 小时</li>
 *     <li>Refresh Token：仅携带 userId、username，默认 7 天</li>
 * </ul>
 */
@Component
public class JwtUtil {

    /** 令牌类型 claim 键 */
    public static final String CLAIM_TOKEN_TYPE = "token_type";

    /** 用户名 claim 键 */
    public static final String CLAIM_USERNAME = "username";

    /** 角色 claim 键 */
    public static final String CLAIM_ROLES = "roles";

    /** 权限 claim 键 */
    public static final String CLAIM_PERMISSIONS = "permissions";

    /** Access Token 类型标识 */
    public static final String TOKEN_TYPE_ACCESS = "access";

    /** Refresh Token 类型标识 */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    /** JWT 签发配置 */
    private final JwtConfig jwtConfig;

    /** HMAC 签名密钥（惰性初始化后缓存） */
    private volatile SecretKey secretKey;

    /**
     * 构造工具类。
     *
     * @param jwtConfig JWT 签发配置
     */
    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 签发 Access Token。
     *
     * @param userId      用户ID
     * @param username    用户名
     * @param roles       角色编码列表
     * @param permissions 权限标识列表
     * @return Access Token 字符串
     */
    public String generateAccessToken(Long userId, String username, List<String> roles, List<String> permissions) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtConfig.getAccessTokenExpire()))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .signWith(key())
                .compact();
    }

    /**
     * 签发 Refresh Token。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return Refresh Token 字符串
     */
    public String generateRefreshToken(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtConfig.getRefreshTokenExpire()))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .signWith(key())
                .compact();
    }

    /**
     * 解析并校验令牌，返回 Claims；令牌非法或过期时抛出 JwtException。
     *
     * @param token 令牌字符串
     * @return Claims 载荷
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 Access Token 有效期（秒）。
     *
     * @return 秒数
     */
    public long getAccessTokenExpireSeconds() {
        return jwtConfig.getAccessTokenExpire() / 1000;
    }

    /**
     * 获取或惰性构建 HMAC 签名密钥。
     *
     * @return 签名密钥
     */
    private SecretKey key() {
        if (secretKey == null) {
            synchronized (this) {
                if (secretKey == null) {
                    secretKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return secretKey;
    }
}
