package com.tianxin.lifeline.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按用户名查询未删除的用户。
     *
     * @param username 登录用户名
     * @return 用户实体，不存在返回 null
     */
    SysUser selectByUsername(@Param("username") String username);
}
