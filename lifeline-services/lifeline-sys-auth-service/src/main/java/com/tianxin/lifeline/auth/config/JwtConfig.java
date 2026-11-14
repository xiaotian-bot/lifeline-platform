package com.tianxin.lifeline.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 签发配置属性（前缀 {@code jwt}）。
 *
 * <p>本服务负责令牌签发，配置项与 application.yml 中 {@code jwt.*} 对应；
 * 校验侧（网关与其他服务）由 lifeline-common-security 的
 * {@code lifeline.security.jwt.*} 承载，两处密钥必须保持一致。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** JWT 签名密钥（HS256 要求 >= 256bit） */
    private String secret;

    /** Access Token 有效期（毫秒），默认 2 小时 */
    private long accessTokenExpire = 7200000L;

    /** Refresh Token 有效期（毫秒），默认 7 天 */
    private long refreshTokenExpire = 604800000L;

    /** JWT 签发者 */
    private String issuer = "lifeline-platform";
}
