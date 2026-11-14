package com.tianxin.lifeline.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 角色-权限关联实体（对应表 sys_role_permission）。
 *
 * <p>种子数据：关联记录ID段 7001 起，管理员（2001）拥有全部权限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class SysRolePermission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long roleId;

    /** 权限ID */
    private Long permissionId;
}
