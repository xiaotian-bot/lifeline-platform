package com.tianxin.lifeline.common.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 用户主体。
 *
 * <p>从 JWT 中解析出的当前登录用户身份信息，存放于
 * {@link com.tianxin.lifeline.common.security.context.SecurityContext} 中，
 * 供业务代码获取当前用户与做权限判断。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 角色编码集合 */
    private Set<String> roles;

    /** 权限标识集合 */
    private Set<String> permissions;

    /**
     * 获取角色集合（空安全）。
     *
     * @return 角色编码集合
     */
    public Set<String> safeRoles() {
        return roles == null ? Collections.emptySet() : roles;
    }

    /**
     * 获取权限集合（空安全）。
     *
     * @return 权限标识集合
     */
    public Set<String> safePermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }
}
