package com.tianxin.lifeline.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录响应 DTO（JWT 双令牌）。
 */
@Data
@Builder
public class LoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色编码列表 */
    private List<String> roles;

    /** 访问令牌（默认 2 小时有效） */
    private String accessToken;

    /** 刷新令牌（默认 7 天有效） */
    private String refreshToken;

    /** 令牌类型 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 访问令牌有效期（秒） */
    private Long expiresIn;
}
