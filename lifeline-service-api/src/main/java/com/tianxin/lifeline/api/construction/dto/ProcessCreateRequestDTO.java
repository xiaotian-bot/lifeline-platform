package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 施工工序创建请求 DTO（服务间共享）。
 */
@Data
public class ProcessCreateRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工序名称 */
    private String processName;

    /** 工序类型：EXCAVATION / PIPE_LAYING / BACKFILL / RESTORE 等 */
    private String processType;
}
