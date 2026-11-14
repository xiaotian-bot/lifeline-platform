package com.tianxin.lifeline.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 系统权限实体（对应表 sys_permission，菜单/按钮）。
 *
 * <p>种子数据：权限ID段 6001 起（一级菜单），6101 起（按钮权限）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 权限类型：菜单 */
    public static final int TYPE_MENU = 1;

    /** 权限类型：按钮 */
    public static final int TYPE_BUTTON = 2;

    /** 顶级菜单的父ID */
    public static final long ROOT_PARENT_ID = 0L;

    /** 权限名称（如：监测大屏） */
    private String permName;

    /** 权限标识（如：monitor:screen） */
    private String permKey;

    /** 权限类型：1 菜单 / 2 按钮 */
    private Integer type;

    /** 父权限ID（0 表示顶级） */
    private Long parentId;

    /** 前端路由路径（菜单类型有效） */
    private String path;

    /** 菜单图标（菜单类型有效） */
    private String icon;

    /** 显示排序号 */
    private Integer sortOrder;
}
