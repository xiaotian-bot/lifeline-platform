package com.tianxin.lifeline.auth.service.impl;

import com.tianxin.lifeline.auth.dto.LoginRequest;
import com.tianxin.lifeline.auth.dto.LoginResponse;
import com.tianxin.lifeline.auth.dto.MenuResponse;
import com.tianxin.lifeline.auth.dto.UserInfoResponse;
import com.tianxin.lifeline.auth.entity.SysPermission;
import com.tianxin.lifeline.auth.entity.SysRole;
import com.tianxin.lifeline.auth.entity.SysUser;
import com.tianxin.lifeline.auth.mapper.SysPermissionMapper;
import com.tianxin.lifeline.auth.mapper.SysRoleMapper;
import com.tianxin.lifeline.auth.mapper.SysUserMapper;
import com.tianxin.lifeline.auth.service.AuthService;
import com.tianxin.lifeline.auth.util.JwtUtil;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务实现。
 *
 * <p>实现登录签发 JWT 双令牌、刷新令牌、用户信息/菜单树查询；
 * 登录时将权限写入 Redis 缓存（{@code auth:perm:{userId}}，TTL 30min），
 * 并异步记录审计日志。
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** 用户 Mapper */
    private final SysUserMapper userMapper;

    /** 角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 权限 Mapper */
    private final SysPermissionMapper permissionMapper;

    /** JWT 工具 */
    private final JwtUtil jwtUtil;

    /** 密码编码器 */
    private final PasswordEncoder passwordEncoder;

    /** 权限缓存 */
    private final PermissionCacheService permissionCacheService;

    /** 审计日志记录器 */
    private final OperLogService operLogService;

    /**
     * 构造认证服务。
     *
     * @param userMapper             用户 Mapper
     * @param roleMapper             角色 Mapper
     * @param permissionMapper       权限 Mapper
     * @param jwtUtil                JWT 工具
     * @param passwordEncoder        密码编码器
     * @param permissionCacheService 权限缓存
     * @param operLogService         审计日志记录器
     */
    public AuthServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper,
                           SysPermissionMapper permissionMapper, JwtUtil jwtUtil,
                           PasswordEncoder passwordEncoder,
                           PermissionCacheService permissionCacheService,
                           OperLogService operLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.permissionCacheService = permissionCacheService;
        this.operLogService = operLogService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResponse login(LoginRequest request, String ip) {
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            operLogService.record(null, "认证中心", "login", "AuthServiceImpl.login",
                    "username=" + request.getUsername(), ip, false, "用户名或密码错误");
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != SysUser.STATUS_ENABLED) {
            operLogService.record(user.getId(), "认证中心", "login", "AuthServiceImpl.login",
                    "username=" + request.getUsername(), ip, false, "账号已被禁用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        List<SysRole> roles = roleMapper.selectRolesByUserId(user.getId());
        List<SysPermission> permissions = permissionMapper.selectPermissionsByUserId(user.getId());
        List<String> roleKeys = roles.stream().map(SysRole::getRoleKey).collect(Collectors.toList());
        List<String> permKeys = permissions.stream().map(SysPermission::getPermKey)
                .distinct().collect(Collectors.toList());

        // 权限写入 Redis 缓存
        permissionCacheService.cache(user.getId(), new LinkedHashSet<>(permKeys));

        // 签发双令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleKeys, permKeys);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        operLogService.record(user.getId(), "认证中心", "login", "AuthServiceImpl.login",
                "username=" + request.getUsername(), ip, true, null);

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roleKeys)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpireSeconds())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }
        Long userId = Long.valueOf(claims.getSubject());
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != SysUser.STATUS_ENABLED) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        List<String> roleKeys = roleMapper.selectRolesByUserId(userId).stream()
                .map(SysRole::getRoleKey).collect(Collectors.toList());
        List<String> permKeys = permissionMapper.selectPermissionsByUserId(userId).stream()
                .map(SysPermission::getPermKey).distinct().collect(Collectors.toList());

        String newAccess = jwtUtil.generateAccessToken(userId, user.getUsername(), roleKeys, permKeys);
        String newRefresh = jwtUtil.generateRefreshToken(userId, user.getUsername());

        return LoginResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roleKeys)
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpireSeconds())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfoResponse currentUserInfo() {
        Long userId = SecurityContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return getUserInfoById(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MenuResponse> currentUserMenus() {
        Long userId = SecurityContext.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return getUserMenus(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfoResponse getUserInfoById(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        Set<String> roleKeys = roleMapper.selectRolesByUserId(userId).stream()
                .map(SysRole::getRoleKey).collect(Collectors.toCollection(LinkedHashSet::new));

        // 权限优先取 Redis 缓存，未命中回源数据库
        Set<String> permKeys = permissionCacheService.get(userId).orElseGet(() -> {
            Set<String> fromDb = permissionMapper.selectPermissionsByUserId(userId).stream()
                    .map(SysPermission::getPermKey).collect(Collectors.toCollection(LinkedHashSet::new));
            permissionCacheService.cache(userId, fromDb);
            return fromDb;
        });

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .roles(roleKeys)
                .permissions(permKeys)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MenuResponse> getUserMenus(Long userId) {
        List<SysPermission> permissions = permissionMapper.selectPermissionsByUserId(userId);
        return buildMenuTree(permissions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getUserRoles(Long userId) {
        return roleMapper.selectRolesByUserId(userId).stream()
                .map(SysRole::getRoleKey).collect(Collectors.toList());
    }

    /**
     * 将权限列表组装为菜单树（顶级 parent_id=0，按钮挂载到所属菜单下）。
     *
     * @param permissions 权限列表（已按 sort_order 排序）
     * @return 菜单树根节点列表
     */
    private List<MenuResponse> buildMenuTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, MenuResponse> nodeMap = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            nodeMap.put(permission.getId(), toMenuNode(permission));
        }
        List<MenuResponse> roots = new ArrayList<>();
        for (SysPermission permission : permissions) {
            MenuResponse node = nodeMap.get(permission.getId());
            Long parentId = permission.getParentId() == null ? SysPermission.ROOT_PARENT_ID : permission.getParentId();
            if (parentId == SysPermission.ROOT_PARENT_ID || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        // 有子节点的目录项标记为 "M"，叶子菜单保持 "C"
        markDirectoryNodes(roots);
        return roots;
    }

    /**
     * 递归标记目录节点：仅当节点拥有非按钮子项（菜单/目录）时才标记为 "M"。
     * 仅有按钮子项的菜单保持 "C"，确保前端侧边栏正常显示。
     *
     * @param nodes 菜单节点列表
     */
    private void markDirectoryNodes(List<MenuResponse> nodes) {
        for (MenuResponse node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                // 检查是否存在非按钮类型的子节点
                boolean hasMenuChild = node.getChildren().stream()
                        .anyMatch(c -> !"F".equals(c.getMenuType()));
                if (hasMenuChild) {
                    node.setMenuType("M");
                }
                markDirectoryNodes(node.getChildren());
            }
        }
    }

    /**
     * 权限实体转菜单树节点。
     *
     * @param permission 权限实体
     * @return 菜单树节点
     */
    private MenuResponse toMenuNode(SysPermission permission) {
        MenuResponse node = new MenuResponse();
        node.setMenuId(permission.getId());
        node.setParentId(permission.getParentId());
        node.setMenuName(permission.getPermName());
        node.setMenuType(permission.getType() != null && permission.getType() == SysPermission.TYPE_BUTTON ? "F" : "C");
        node.setPath(permission.getPath());
        node.setPermission(permission.getPermKey());
        node.setIcon(permission.getIcon());
        node.setSortOrder(permission.getSortOrder());
        return node;
    }
}
