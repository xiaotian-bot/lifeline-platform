package com.tianxin.lifeline.auth.service;

import com.tianxin.lifeline.auth.dto.LoginRequest;
import com.tianxin.lifeline.auth.dto.LoginResponse;
import com.tianxin.lifeline.auth.dto.MenuResponse;
import com.tianxin.lifeline.auth.dto.UserInfoResponse;

import java.util.List;

/**
 * 认证服务接口。
 *
 * <p>负责登录签发 JWT 双令牌、刷新令牌、当前用户信息/菜单查询，
 * 以及对外（Feign）提供的按用户ID查询能力。
 */
public interface AuthService {

    /**
     * 登录并签发双令牌。
     *
     * @param request 登录请求
     * @param ip      客户端IP（用于审计日志）
     * @return 登录响应（含 Access/Refresh Token）
     */
    LoginResponse login(LoginRequest request, String ip);

    /**
     * 使用 Refresh Token 换取新的 Access Token。
     *
     * @param refreshToken 刷新令牌
     * @return 登录响应（新的双令牌）
     */
    LoginResponse refresh(String refreshToken);

    /**
     * 获取当前登录用户信息（含角色与权限）。
     *
     * @return 用户信息
     */
    UserInfoResponse currentUserInfo();

    /**
     * 获取当前登录用户的菜单权限树。
     *
     * @return 菜单树
     */
    List<MenuResponse> currentUserMenus();

    /**
     * 按用户ID获取用户信息（含角色与权限，供 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserInfoResponse getUserInfoById(Long userId);

    /**
     * 按用户ID获取菜单权限树（供 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 菜单树
     */
    List<MenuResponse> getUserMenus(Long userId);

    /**
     * 按用户ID获取角色编码列表（供 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> getUserRoles(Long userId);
}
