package com.tianxin.lifeline.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.auth.dto.PermissionAssignRequest;
import com.tianxin.lifeline.auth.entity.SysPermission;
import com.tianxin.lifeline.auth.entity.SysRole;
import com.tianxin.lifeline.auth.entity.SysRolePermission;
import com.tianxin.lifeline.auth.mapper.SysPermissionMapper;
import com.tianxin.lifeline.auth.mapper.SysRoleMapper;
import com.tianxin.lifeline.auth.mapper.SysRolePermissionMapper;
import com.tianxin.lifeline.auth.service.RoleService;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与权限管理服务实现。
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    /** 角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 权限 Mapper */
    private final SysPermissionMapper permissionMapper;

    /** 角色-权限关联 Mapper */
    private final SysRolePermissionMapper rolePermissionMapper;

    /** 权限缓存 */
    private final PermissionCacheService permissionCacheService;

    /** 审计日志记录器 */
    private final OperLogService operLogService;

    /**
     * 构造角色权限服务。
     *
     * @param roleMapper           角色 Mapper
     * @param permissionMapper     权限 Mapper
     * @param rolePermissionMapper 角色-权限关联 Mapper
     * @param permissionCacheService 权限缓存
     * @param operLogService       审计日志记录器
     */
    public RoleServiceImpl(SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
                           SysRolePermissionMapper rolePermissionMapper,
                           PermissionCacheService permissionCacheService,
                           OperLogService operLogService) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionCacheService = permissionCacheService;
        this.operLogService = operLogService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortOrder));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(PermissionAssignRequest request) {
        // 角色存在性校验
        SysRole role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不存在");
        }

        // 权限存在性校验
        List<Long> permissionIds = request.getPermissionIds().stream().distinct().collect(Collectors.toList());
        List<SysPermission> permissions = permissionMapper.selectBatchIds(permissionIds);
        Set<Long> validIds = permissions.stream().map(SysPermission::getId).collect(Collectors.toSet());
        List<Long> invalidIds = permissionIds.stream().filter(id -> !validIds.contains(id)).toList();
        if (!invalidIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "存在非法权限ID: " + invalidIds);
        }

        // 全量覆盖：先逻辑删除原有关联，再插入新关联
        rolePermissionMapper.logicDeleteByRoleId(request.getRoleId());
        for (Long permissionId : permissionIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(request.getRoleId());
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }

        // 权限变更后清空缓存，保证后续鉴权一致性
        permissionCacheService.evictAll();

        operLogService.record(SecurityContext.getUserId(), "权限管理", "assign_permissions",
                "RoleServiceImpl.assignPermissions",
                "roleId=" + request.getRoleId() + ", permissionIds=" + permissionIds, null, true, null);
        log.info("权限分配成功 roleId={}, permissionCount={}", request.getRoleId(), permissionIds.size());
    }
}
