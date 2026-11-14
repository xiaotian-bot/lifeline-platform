package com.tianxin.lifeline.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色 Mapper。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询指定用户绑定的角色列表。
     *
     * <p>本平台未设独立用户-角色关联表，用户与角色通过
     * {@code sys_user.dept_id = sys_role.id + 7000} 的约定绑定
     * （种子数据：dept_id 9001-9005 对应角色 2001-2005）。
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 按角色编码查询启用的角色。
     *
     * @param roleKey 角色编码
     * @return 角色实体，不存在返回 null
     */
    SysRole selectByRoleKey(@Param("roleKey") String roleKey);
}
