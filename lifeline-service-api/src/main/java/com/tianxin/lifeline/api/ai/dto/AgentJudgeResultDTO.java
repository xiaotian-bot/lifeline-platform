package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 预警研判代理结果 DTO（服务间共享）。
 *
 * <p>agent-service POST /api/v1/ai/agent/judge 的出参契约。
 */
@Data
public class AgentJudgeResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 根因分析 */
    private String rootCause;

    /** 预警等级复核意见（维持/升级/降级及理由） */
    private String levelReview;

    /** 影响范围评估 */
    private String impactScope;

    /** 趋势预测 */
    private String trendPrediction;

    /** 处置优先级：如 HIGH/MEDIUM/LOW */
    private String handlePriority;

    /** 置信度（0-1） */
    private Double confidence;
}
