package com.tianxin.lifeline.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 权限分配请求 DTO（为角色授予权限集合）。
 */
@Data
public class PermissionAssignRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标角色ID */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 授予的权限ID列表（全量覆盖） */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
}
