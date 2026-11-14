package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工序人工质检记录提交请求 DTO（服务间共享）。
 */
@Data
public class ProcessInspectRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 质检人 */
    private String inspector;

    /** 质检得分（百分制，0-100） */
    private BigDecimal qualityScore;
}
