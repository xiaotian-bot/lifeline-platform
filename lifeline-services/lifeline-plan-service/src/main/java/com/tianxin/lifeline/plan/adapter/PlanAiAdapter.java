package com.tianxin.lifeline.plan.adapter;

import com.tianxin.lifeline.api.ai.dto.PlanAgentResultDTO;
import com.tianxin.lifeline.plan.dto.PlanGenerateRequest;

/**
 * AI 预案生成适配器接口。
 *
 * <p>预案生成时调用，组装 {@code PlanAgentRequestDTO} 经
 * ai-gateway-service 智能预案代理产出任务/资源/处置建议。
 * 契约约定：实现必须保证「失败可降级」——AI 服务不可用、超时、
 * 返回失败或数据为空时返回 null，由调用方降级为按模板 content
 * 实例化通用预案，预案生成接口永不失败。
 */
public interface PlanAiAdapter {

    /**
     * 调用 AI 预案代理生成预案建议。
     *
     * @param request 预案生成请求（含预警上下文）
     * @return AI 预案生成结果，AI 不可用或降级时返回 null
     */
    PlanAgentResultDTO generate(PlanGenerateRequest request);
}
