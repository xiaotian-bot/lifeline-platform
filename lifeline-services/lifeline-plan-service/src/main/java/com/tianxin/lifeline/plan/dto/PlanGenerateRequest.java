package com.tianxin.lifeline.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案生成请求。
 *
 * <p>warningEventId 必填用于关联预警事件；templateId 与 scenarioType
 * 至少提供其一（templateId 优先），用于定位实例化的预案模板。
 * 其余为可选预警上下文，透传给 AI 预案代理提升生成针对性。
 */
@Data
public class PlanGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联预警事件ID */
    @NotNull(message = "预警事件ID不能为空")
    private Long warningEventId;

    /** 场景类型：GAS_LEAK/WATER_BURST/HEATING_LEAK/DRAINAGE_OVERFLOW/
     * ROAD_COLLAPSE/THIRD_PARTY_DAMAGE/CORROSION_FAILURE/COMPREHENSIVE */
    private String scenarioType;

    /** 指定预案模板ID（优先于 scenarioType） */
    private Long templateId;

    /** 事发位置描述 */
    private String locationDesc;

    /** 管网类型：gas/water/drainage/heating/storm（可选，透传 AI） */
    private String pipeType;

    /** 预警等级：1红/2橙/3黄/4蓝（可选，透传 AI） */
    private Integer warningLevel;

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage（可选，透传 AI） */
    private String metricType;

    /** 预警标题（可选，透传 AI） */
    private String title;

    /** 预警描述（可选，透传 AI，缺省时由位置描述拼装） */
    private String description;
}
