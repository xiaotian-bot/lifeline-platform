package com.tianxin.lifeline.aigateway.dto;

import com.tianxin.lifeline.api.ai.dto.AnomalyResultDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 批量异常检测单条结果项（网关内部 DTO）。
 */
@Data
public class BatchDetectItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否成功（上游正常返回；降级项为 false） */
    private Boolean success;

    /** 结果说明（降级项为降级原因） */
    private String message;

    /** 检测结果（降级项为 null） */
    private AnomalyResultDTO data;
}
