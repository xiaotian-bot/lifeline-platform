package com.tianxin.lifeline.api.auth.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 菜单权限 DTO（服务间共享）。
 */
@Data
public class MenuDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    private Long menuId;

    /** 父菜单ID（0 表示顶级） */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 菜单类型：M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 路由路径 */
    private String path;

    /** 权限标识 */
    private String permission;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 子菜单 */
    private List<MenuDTO> children;
}
