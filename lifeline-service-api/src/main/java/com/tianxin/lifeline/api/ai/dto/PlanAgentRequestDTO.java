package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI预案生成代理请求 DTO（服务间共享）。
 *
 * <p>预警事件确认后，由预案/运维服务携带预警上下文调用
 * ai-gateway-service 的智能预案代理，生成处置预案建议。
 */
@Data
public class PlanAgentRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件ID */
    private Long warningId;

    /** 管网类型：gas/water/drainage/heating/storm */
    private String pipeType;

    /** 预警等级：1红/2橙/3黄/4蓝 */
    private Integer warningLevel;

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String metricType;

    /** 预警标题 */
    private String title;

    /** 预警描述（触发详情、位置等上下文） */
    private String description;
}
