package com.tianxin.lifeline.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tianxin.lifeline.auth.dto.UserCreateRequest;
import com.tianxin.lifeline.auth.entity.SysRole;
import com.tianxin.lifeline.auth.entity.SysUser;
import com.tianxin.lifeline.auth.mapper.SysRoleMapper;
import com.tianxin.lifeline.auth.mapper.SysUserMapper;
import com.tianxin.lifeline.auth.service.UserService;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理服务实现。
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /** 用户-角色绑定的 dept_id 偏移量（dept_id = roleId + 7000） */
    private static final long DEPT_ROLE_OFFSET = 7000L;

    /** 用户 Mapper */
    private final SysUserMapper userMapper;

    /** 角色 Mapper */
    private final SysRoleMapper roleMapper;

    /** 密码编码器 */
    private final PasswordEncoder passwordEncoder;

    /** 审计日志记录器 */
    private final OperLogService operLogService;

    /**
     * 构造用户管理服务。
     *
     * @param userMapper      用户 Mapper
     * @param roleMapper      角色 Mapper
     * @param passwordEncoder 密码编码器
     * @param operLogService  审计日志记录器
     */
    public UserServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper,
                           PasswordEncoder passwordEncoder, OperLogService operLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.operLogService = operLogService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long createUser(UserCreateRequest request) {
        // 用户名唯一性校验
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名已存在");
        }

        // 角色存在性校验
        SysRole role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus() == null ? SysUser.STATUS_ENABLED : request.getStatus());
        // 通过 dept_id = roleId + 7000 约定绑定角色
        user.setDeptId(role.getId() + DEPT_ROLE_OFFSET);
        userMapper.insert(user);

        operLogService.record(SecurityContext.getUserId(), "用户管理", "create_user",
                "UserServiceImpl.createUser", "username=" + request.getUsername(), null, true, null);
        log.info("创建用户成功 id={}, username={}, roleKey={}", user.getId(), user.getUsername(), role.getRoleKey());
        return user.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<Map<String, Object>> pageUsers(PageQuery query) {
        int page = query.getPage() == null ? 1 : query.getPage();
        int size = query.getSize() == null ? 20 : query.getSize();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysUser::getId);

        Long total = userMapper.selectCount(new LambdaQueryWrapper<>());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> pageObj =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> result = userMapper.selectPage(pageObj, wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        for (SysUser u : result.getRecords()) {
            Map<String, Object> row = new HashMap<>();
            row.put("userId", u.getId());
            row.put("username", u.getUsername());
            row.put("realName", u.getRealName());
            row.put("phone", u.getPhone());
            row.put("email", u.getEmail());
            row.put("status", u.getStatus());
            row.put("createTime", u.getCreatedAt() != null ? u.getCreatedAt().toString() : null);
            // 角色名称通过 deptId 反查
            if (u.getDeptId() != null && u.getDeptId() > DEPT_ROLE_OFFSET) {
                Long roleId = u.getDeptId() - DEPT_ROLE_OFFSET;
                SysRole role = roleMapper.selectById(roleId);
                row.put("roleId", roleId);
                row.put("roleName", role != null ? role.getRoleName() : "-");
            } else {
                row.put("roleId", null);
                row.put("roleName", "-");
            }
            records.add(row);
        }
        return PageResult.of(records, total != null ? total : 0L, page, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateUser(Long userId, Map<String, Object> updates) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId);
        if (updates.containsKey("realName")) {
            wrapper.set(SysUser::getRealName, updates.get("realName"));
        }
        if (updates.containsKey("phone")) {
            wrapper.set(SysUser::getPhone, updates.get("phone"));
        }
        if (updates.containsKey("email")) {
            wrapper.set(SysUser::getEmail, updates.get("email"));
        }
        if (updates.containsKey("status")) {
            wrapper.set(SysUser::getStatus, updates.get("status"));
        }
        if (updates.containsKey("roleId")) {
            Object roleIdObj = updates.get("roleId");
            if (roleIdObj != null) {
                long roleId = Long.parseLong(roleIdObj.toString());
                wrapper.set(SysUser::getDeptId, roleId + DEPT_ROLE_OFFSET);
            }
        }
        userMapper.update(null, wrapper);
        log.info("更新用户成功 userId={}", userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetPassword(Long userId, String password) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId)
               .set(SysUser::getPassword, passwordEncoder.encode(password));
        userMapper.update(null, wrapper);
        log.info("重置用户密码成功 userId={}", userId);
    }
}
