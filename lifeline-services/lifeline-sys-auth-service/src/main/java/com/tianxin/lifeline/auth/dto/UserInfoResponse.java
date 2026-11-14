package com.tianxin.lifeline.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户信息响应 DTO（用户 + 角色 + 权限）。
 */
@Data
@Builder
public class UserInfoResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 所属部门ID */
    private Long deptId;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 角色编码集合 */
    private Set<String> roles;

    /** 权限标识集合 */
    private Set<String> permissions;
}
