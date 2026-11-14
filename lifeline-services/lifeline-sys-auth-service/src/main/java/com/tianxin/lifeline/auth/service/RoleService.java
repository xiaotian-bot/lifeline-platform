package com.tianxin.lifeline.auth.service;

import com.tianxin.lifeline.auth.dto.PermissionAssignRequest;
import com.tianxin.lifeline.auth.entity.SysRole;

import java.util.List;

/**
 * 角色与权限管理服务接口。
 */
public interface RoleService {

    /**
     * 查询全部启用的角色列表。
     *
     * @return 角色列表
     */
    List<SysRole> listRoles();

    /**
     * 为角色分配权限（全量覆盖原有关联）。
     *
     * @param request 权限分配请求
     */
    void assignPermissions(PermissionAssignRequest request);
}
