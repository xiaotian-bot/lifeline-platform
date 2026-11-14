package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工序 AI 视觉质检结果 DTO（服务间共享）。
 */
@Data
public class VisionInspectResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工序ID */
    private Long processId;

    /** 工序质检状态（回写后） */
    private Integer processStatus;

    /** 工序质检状态中文文本 */
    private String processStatusText;

    /** 是否 AI 降级（未识别，转人工复核） */
    private Boolean degraded;

    /** 是否识别到违规 */
    private Boolean violation;

    /** 违规类型 */
    private String violationType;

    /** 识别置信度（0-1） */
    private Double confidence;

    /** 证据影像对象键 */
    private String evidenceUrl;

    /** 违规/复核描述 */
    private String description;

    /** 违规记录ID */
    private Long violationId;

    /** 质检得分 */
    private BigDecimal qualityScore;
}
