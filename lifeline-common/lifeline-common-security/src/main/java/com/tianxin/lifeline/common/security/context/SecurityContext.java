package com.tianxin.lifeline.common.security.context;

import com.tianxin.lifeline.common.security.dto.UserPrincipal;

import java.util.Collections;
import java.util.Set;

/**
 * 安全上下文工具。
 *
 * <p>基于 ThreadLocal 存储当前请求线程的用户主体（由 JwtAuthFilter 写入），
 * 业务代码通过静态方法获取当前登录用户信息与做角色/权限判断。
 * 请求结束时必须调用 {@link #clear()} 防止线程池复用导致的信息串号。
 */
public final class SecurityContext {

    /** 当前线程的用户主体 */
    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private SecurityContext() {
    }

    /**
     * 设置当前线程的用户主体。
     *
     * @param principal 用户主体
     */
    public static void set(UserPrincipal principal) {
        HOLDER.set(principal);
    }

    /**
     * 获取当前线程的用户主体。
     *
     * @return 用户主体，未登录返回 null
     */
    public static UserPrincipal get() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程的用户主体（请求结束必须调用）。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 用户ID，未登录返回 null
     */
    public static Long getUserId() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.getUserId();
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名，未登录返回 null
     */
    public static String getUsername() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.getUsername();
    }

    /**
     * 获取当前登录用户角色集合。
     *
     * @return 角色编码集合，未登录返回空集合
     */
    public static Set<String> getRoles() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? Collections.emptySet() : principal.safeRoles();
    }

    /**
     * 获取当前登录用户权限集合。
     *
     * @return 权限标识集合，未登录返回空集合
     */
    public static Set<String> getPermissions() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? Collections.emptySet() : principal.safePermissions();
    }

    /**
     * 判断当前用户是否拥有指定角色。
     *
     * @param role 角色编码
     * @return true 表示拥有
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * 判断当前用户是否拥有指定权限。
     *
     * @param permission 权限标识
     * @return true 表示拥有
     */
    public static boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    /**
     * 判断当前线程是否已认证。
     *
     * @return true 表示已登录
     */
    public static boolean isAuthenticated() {
        return HOLDER.get() != null;
    }
}
