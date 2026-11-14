package com.tianxin.lifeline.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 安全配置属性。
 *
 * <p>配置前缀 {@code lifeline.security.jwt}，示例：
 * <pre>
 * lifeline:
 *   security:
 *     jwt:
 *       secret: xxx
 *       access-token-expire: 2h
 *       refresh-token-expire: 7d
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.security.jwt")
public class SecurityProperties {

    /** JWT 签名密钥（HS256 要求 >= 256bit，生产环境必须外部化配置） */
    private String secret = "lifeline-platform-default-secret-key-must-be-at-least-256-bits-long";

    /** Access Token 有效期，默认 2 小时 */
    private Duration accessTokenExpire = Duration.ofHours(2);

    /** Refresh Token 有效期，默认 7 天 */
    private Duration refreshTokenExpire = Duration.ofDays(7);

    /** JWT 签发者 */
    private String issuer = "lifeline-platform";
}
