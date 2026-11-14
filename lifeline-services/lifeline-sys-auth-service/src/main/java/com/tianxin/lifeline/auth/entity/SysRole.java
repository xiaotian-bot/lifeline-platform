package com.tianxin.lifeline.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 系统角色实体（对应表 sys_role）。
 *
 * <p>种子数据：角色ID段 2001-2005
 * （ROLE_ADMIN/ROLE_GOVERNMENT/ROLE_OPERATOR/ROLE_ENTERPRISE/ROLE_GUEST）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 超级管理员角色编码 */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /** 角色名称（如：超级管理员） */
    private String roleName;

    /** 角色编码（如：ROLE_ADMIN） */
    private String roleKey;

    /** 显示排序号 */
    private Integer sortOrder;

    /** 角色状态：0 禁用 / 1 启用 */
    private Integer status;
}
