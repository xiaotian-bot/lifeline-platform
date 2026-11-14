package com.tianxin.lifeline.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.auth.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联 Mapper。
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 逻辑删除指定角色的全部权限关联（权限重分配前调用）。
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    int logicDeleteByRoleId(@Param("roleId") Long roleId);
}
