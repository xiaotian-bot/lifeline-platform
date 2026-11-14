package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 预警研判代理请求 DTO（服务间共享）。
 *
 * <p>预警事件触发后，由预警服务携带事件上下文调用
 * ai-gateway-service 的智能研判代理（转发 agent-service
 * POST /api/v1/ai/agent/judge），输出根因分析与处置建议。
 */
@Data
public class AgentJudgeRequestDTO implements Serializable {

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

    /** 触发值 */
    private BigDecimal triggerValue;

    /** 规则阈值 */
    private BigDecimal threshold;

    /** 预警标题 */
    private String title;

    /** 预警描述（触发详情、位置等上下文） */
    private String description;
}
