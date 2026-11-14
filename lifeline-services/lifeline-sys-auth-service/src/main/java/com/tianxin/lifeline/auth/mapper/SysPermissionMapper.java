package com.tianxin.lifeline.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.auth.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限 Mapper。
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 查询指定用户拥有的全部权限（菜单 + 按钮）。
     *
     * <p>关联链：sys_user → sys_role（dept_id 约定） → sys_role_permission → sys_permission。
     *
     * @param userId 用户ID
     * @return 权限列表（按 sort_order 升序）
     */
    List<SysPermission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 查询指定角色集合拥有的全部权限。
     *
     * @param roleIds 角色ID列表
     * @return 权限列表（按 sort_order 升序）
     */
    List<SysPermission> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
