package com.tianxin.lifeline.api.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预案模板 DTO（服务间共享）。
 *
 * <p>镜像 plan-service 的 PlanTemplate 实体，供跨服务调用序列化/反序列化。
 */
@Data
public class PlanTemplateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板类型：EMERGENCY 应急处置预案 */
    private String templateType;

    /** 场景类型：GAS_LEAK/WATER_BURST/HEATING_LEAK/DRAINAGE_OVERFLOW/
     * ROAD_COLLAPSE/THIRD_PARTY_DAMAGE/CORROSION_FAILURE/COMPREHENSIVE */
    private String scenarioType;

    /** 预案内容（处置要点文本） */
    private String content;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
