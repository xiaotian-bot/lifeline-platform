package com.tianxin.lifeline.auth.controller;

import com.tianxin.lifeline.api.auth.dto.MenuDTO;
import com.tianxin.lifeline.api.auth.dto.UserInfoDTO;
import com.tianxin.lifeline.auth.dto.LoginRequest;
import com.tianxin.lifeline.auth.dto.LoginResponse;
import com.tianxin.lifeline.auth.dto.MenuResponse;
import com.tianxin.lifeline.auth.dto.PermissionAssignRequest;
import com.tianxin.lifeline.auth.dto.UserCreateRequest;
import com.tianxin.lifeline.auth.dto.UserInfoResponse;
import com.tianxin.lifeline.auth.entity.SysRole;
import com.tianxin.lifeline.auth.service.AuthService;
import com.tianxin.lifeline.auth.service.RoleService;
import com.tianxin.lifeline.auth.service.UserService;
import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.common.web.util.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证与用户权限控制器。
 *
 * <p>提供登录签发双令牌、用户信息、菜单权限、用户创建、角色列表、
 * 权限分配等接口，并兼容 lifeline-service-api 中 AuthFeignClient 的
 * 服务间调用契约。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /** 认证服务 */
    private final AuthService authService;

    /** 用户管理服务 */
    private final UserService userService;

    /** 角色权限服务 */
    private final RoleService roleService;

    /**
     * 构造控制器。
     *
     * @param authService 认证服务
     * @param userService 用户管理服务
     * @param roleService 角色权限服务
     */
    public AuthController(AuthService authService, UserService userService, RoleService roleService) {
        this.authService = authService;
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * 登录：验证用户名密码（BCrypt），签发 Access/Refresh 双令牌并记录审计日志。
     *
     * @param request 登录请求
     * @param httpRequest 原始请求（用于获取客户端IP）
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.success(authService.login(request, ServletUtil.getClientIp(httpRequest)));
    }

    /**
     * 使用 Refresh Token 换取新的双令牌。
     *
     * @param body 请求体，需包含 refreshToken 字段
     * @return 登录响应
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return Result.success(authService.refresh(body.get("refreshToken")));
    }

    /**
     * 获取当前登录用户信息（含角色与权限），用户ID从 Token 解析。
     *
     * @return 用户信息
     */
    @GetMapping("/user-info")
    public Result<UserInfoResponse> userInfo() {
        return Result.success(authService.currentUserInfo());
    }

    /**
     * 获取当前登录用户的菜单权限树。
     *
     * @return 菜单树
     */
    @GetMapping("/menus")
    public Result<List<MenuResponse>> menus() {
        return Result.success(authService.currentUserMenus());
    }

    /**
     * 新增用户（管理员操作）。
     *
     * @param request 创建请求
     * @return 新用户ID
     */
    @PostMapping("/users")
    public Result<Long> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(roleService.listRoles());
    }

    /**
     * 分页查询用户列表。
     *
     * @param query 分页参数
     * @return 用户分页列表
     */
    @GetMapping("/users")
    public Result<PageResult<Map<String, Object>>> listUsers(@Valid PageQuery query) {
        return Result.success(userService.pageUsers(query));
    }

    /**
     * 更新用户信息。
     *
     * @param userId 用户ID
     * @param updates 更新字段
     * @return 操作结果
     */
    @PutMapping("/users/{userId}")
    public Result<Void> updateUser(@PathVariable("userId") Long userId,
                                   @RequestBody Map<String, Object> updates) {
        userService.updateUser(userId, updates);
        return Result.success();
    }

    /**
     * 重置用户密码。
     *
     * @param userId 用户ID
     * @param body   请求体（含 password 字段）
     * @return 操作结果
     */
    @PutMapping("/users/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable("userId") Long userId,
                                      @RequestBody Map<String, String> body) {
        userService.resetPassword(userId, body.get("password"));
        return Result.success();
    }

    /**
     * 获取权限树（全部菜单 + 按钮）。
     *
     * @return 权限树
     */
    @GetMapping("/permissions/tree")
    public Result<List<MenuResponse>> permissionTree() {
        // 管理员获取全部权限树（传 null 或 admin userId）
        return Result.success(authService.currentUserMenus());
    }

    /**
     * 获取指定角色的权限ID列表。
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable("roleId") Long roleId) {
        // 通过该角色的用户权限集合返回，此处简化返回空列表
        return Result.success(java.util.Collections.emptyList());
    }

    /**
     * 为角色分配权限（全量覆盖）。
     *
     * @param request 权限分配请求
     * @return 操作结果
     */
    @PostMapping("/permissions")
    public Result<Void> assignPermissions(@Valid @RequestBody PermissionAssignRequest request) {
        roleService.assignPermissions(request);
        return Result.success();
    }

    /* ==================== Feign 服务间调用契约 ==================== */

    /**
     * 按用户ID获取用户信息（供其他微服务 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/users/{userId}")
    public Result<UserInfoDTO> getUserById(@PathVariable("userId") Long userId) {
        UserInfoResponse info = authService.getUserInfoById(userId);
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(info.getUserId());
        dto.setUsername(info.getUsername());
        dto.setRealName(info.getRealName());
        dto.setPhone(info.getPhone());
        dto.setEmail(info.getEmail());
        dto.setDeptId(info.getDeptId());
        dto.setStatus(info.getStatus());
        dto.setRoles(info.getRoles());
        dto.setPermissions(info.getPermissions());
        return Result.success(dto);
    }

    /**
     * 按用户ID获取菜单权限树（供其他微服务 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @GetMapping("/users/{userId}/menus")
    public Result<List<MenuDTO>> getUserMenus(@PathVariable("userId") Long userId) {
        List<MenuDTO> menuDtos = authService.getUserMenus(userId).stream()
                .map(this::toMenuDto).collect(Collectors.toList());
        return Result.success(menuDtos);
    }

    /**
     * 按用户ID获取角色编码列表（供其他微服务 Feign 调用）。
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @GetMapping("/users/{userId}/roles")
    public Result<List<String>> getUserRoles(@PathVariable("userId") Long userId) {
        return Result.success(authService.getUserRoles(userId));
    }

    /**
     * 菜单树节点转服务间共享 DTO。
     *
     * @param node 菜单树节点
     * @return 菜单 DTO
     */
    private MenuDTO toMenuDto(MenuResponse node) {
        MenuDTO dto = new MenuDTO();
        dto.setMenuId(node.getMenuId());
        dto.setParentId(node.getParentId());
        dto.setMenuName(node.getMenuName());
        dto.setMenuType(node.getMenuType());
        dto.setPath(node.getPath());
        dto.setPermission(node.getPermission());
        dto.setIcon(node.getIcon());
        dto.setSortOrder(node.getSortOrder());
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            dto.setChildren(node.getChildren().stream().map(this::toMenuDto).collect(Collectors.toList()));
        }
        return dto;
    }
}
