package com.tianxin.lifeline.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工序 AI 视觉质检结果视图。
 *
 * <p>封装视觉分析结论与工序质检回写结果；AI 服务不可用时
 * {@link #degraded} 为 true，表示「未识别，转人工复核」降级结论。
 */
@Data
public class VisionInspectResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工序ID */
    private Long processId;

    /** 工序质检状态（回写后）：0 待质检 / 1 质检中 / 2 质检通过 / 3 质检不通过 */
    private Integer processStatus;

    /** 工序质检状态中文文本 */
    private String processStatusText;

    /** 是否 AI 降级（未识别，转人工复核） */
    private Boolean degraded;

    /** 是否识别到违规 */
    private Boolean violation;

    /** 违规类型（命中违规时） */
    private String violationType;

    /** 识别置信度（0-1） */
    private Double confidence;

    /** 证据影像对象键 */
    private String evidenceUrl;

    /** 违规/复核描述 */
    private String description;

    /** 违规记录ID（命中违规落库后回填） */
    private Long violationId;

    /** 质检得分（置信度折算百分制，降级时为空） */
    private BigDecimal qualityScore;
}
