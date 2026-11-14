package com.tianxin.lifeline.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 系统用户实体（对应表 sys_user）。
 *
 * <p>种子数据：用户ID段 1001-1005（admin/government/operator/enterprise/guest），
 * 密码为明文 {@code 123456} 的 BCrypt 哈希。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 账号状态：启用 */
    public static final int STATUS_ENABLED = 1;

    /** 账号状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 登录用户名（唯一） */
    private String username;

    /** 密码（BCrypt 哈希），序列化时忽略 */
    @JsonIgnore
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 账号状态：0 禁用 / 1 启用 */
    private Integer status;

    /** 所属部门ID（本平台中同时承担用户-角色绑定职责：dept_id = 角色ID + 7000） */
    private Long deptId;
}
