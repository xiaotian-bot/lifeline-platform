package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 研判-预案一体化工作流结果 DTO（服务间共享）。
 *
 * <p>agent-service POST /api/v1/ai/agent/workflow 的出参契约：
 * 入参同 {@link AgentJudgeRequestDTO}，一次调用完成「深度研判 + 预案生成」，
 * 返回 judge 与 plan 两部分合并结构。
 */
@Data
public class AgentWorkflowResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 深度研判结果 */
    private AgentJudgeResultDTO judge;

    /** 智能预案生成结果 */
    private PlanAgentResultDTO plan;
}
