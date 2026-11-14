package com.tianxin.lifeline.auth.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限树节点响应 DTO。
 */
@Data
public class MenuResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单/权限ID */
    private Long menuId;

    /** 父级ID（0 表示顶级） */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型：M 目录 / C 菜单 / F 按钮 */
    private String menuType;

    /** 前端路由路径 */
    private String path;

    /** 权限标识（perm_key） */
    private String permission;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 子节点 */
    private List<MenuResponse> children = new ArrayList<>();
}
