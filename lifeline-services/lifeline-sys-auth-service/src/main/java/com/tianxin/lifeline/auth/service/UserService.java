package com.tianxin.lifeline.auth.service;

import com.tianxin.lifeline.auth.dto.UserCreateRequest;
import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.auth.entity.SysUser;

import java.util.Map;

/**
 * 用户管理服务接口。
 */
public interface UserService {

    /**
     * 创建用户（管理员操作）。
     *
     * <p>校验用户名唯一性，密码以 BCrypt 加密存储，
     * 并按 roleId 绑定角色（通过 dept_id = roleId + 7000 约定）。
     *
     * @param request 创建请求
     * @return 新用户ID
     */
    Long createUser(UserCreateRequest request);

    /**
     * 分页查询用户列表。
     *
     * @param query 分页与过滤条件
     * @return 分页结果
     */
    PageResult<Map<String, Object>> pageUsers(PageQuery query);

    /**
     * 更新用户信息。
     *
     * @param userId 用户ID
     * @param updates 更新字段
     */
    void updateUser(Long userId, Map<String, Object> updates);

    /**
     * 重置用户密码。
     *
     * @param userId   用户ID
     * @param password 新密码（明文）
     */
    void resetPassword(Long userId, String password);
}
