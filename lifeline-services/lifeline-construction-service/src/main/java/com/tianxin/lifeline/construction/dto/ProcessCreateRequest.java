package com.tianxin.lifeline.construction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 施工工序创建请求。
 */
@Data
public class ProcessCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工序名称 */
    @NotBlank(message = "工序名称不能为空")
    @Size(max = 128, message = "工序名称长度不能超过128")
    private String processName;

    /** 工序类型：EXCAVATION 开挖 / PIPE_LAYING 管道铺设 / BACKFILL 回填 / RESTORE 路面恢复 等 */
    @NotBlank(message = "工序类型不能为空")
    @Size(max = 32, message = "工序类型长度不能超过32")
    private String processType;
}
