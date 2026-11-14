package com.tianxin.lifeline.api.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案生成请求 DTO（服务间共享）。
 */
@Data
public class PlanGenerateRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联预警事件ID */
    private Long warningEventId;

    /** 场景类型 */
    private String scenarioType;

    /** 指定预案模板ID */
    private Long templateId;

    /** 事发位置描述 */
    private String locationDesc;

    /** 管网类型 */
    private String pipeType;

    /** 预警等级：1红/2橙/3黄/4蓝 */
    private Integer warningLevel;

    /** 指标类型 */
    private String metricType;

    /** 预警标题 */
    private String title;

    /** 预警描述 */
    private String description;
}
