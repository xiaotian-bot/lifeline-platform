package com.tianxin.lifeline.plan.adapter;

import com.tianxin.lifeline.api.ai.AiGatewayFeignClient;
import com.tianxin.lifeline.api.ai.dto.PlanAgentRequestDTO;
import com.tianxin.lifeline.api.ai.dto.PlanAgentResultDTO;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.plan.dto.PlanGenerateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI 预案生成适配器默认实现（失败降级）。
 *
 * <p>组装预警上下文为 {@link PlanAgentRequestDTO}，调用
 * {@link AiGatewayFeignClient#generatePlan} 获取智能预案建议。
 *
 * <p>「失败即降级」契约：AI 服务不可用、调用异常、返回失败或
 * 结果数据无效（无任务清单）时统一返回 null，由预案生成服务降级为
 * 按模板 content 实例化通用预案，保证预案生成接口永不失败。
 */
@Slf4j
@Component
public class DefaultPlanAiAdapter implements PlanAiAdapter {

    /** AI 能力网关 Feign 客户端 */
    private final AiGatewayFeignClient aiGatewayFeignClient;

    /**
     * 构造适配器。
     *
     * @param aiGatewayFeignClient AI 能力网关 Feign 客户端
     */
    public DefaultPlanAiAdapter(AiGatewayFeignClient aiGatewayFeignClient) {
        this.aiGatewayFeignClient = aiGatewayFeignClient;
    }

    /**
     * 调用 AI 预案代理生成预案建议（失败降级返回 null）。
     *
     * @param request 预案生成请求（含预警上下文）
     * @return AI 预案生成结果，AI 不可用或降级时返回 null
     */
    @Override
    public PlanAgentResultDTO generate(PlanGenerateRequest request) {
        if (request == null) {
            return null;
        }
        try {
            PlanAgentRequestDTO agentRequest = buildAgentRequest(request);
            Result<PlanAgentResultDTO> result = aiGatewayFeignClient.generatePlan(agentRequest);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.warn("AI 预案生成无有效结果, 降级模板实例化: warningEventId={}", request.getWarningEventId());
                return null;
            }
            PlanAgentResultDTO data = result.getData();
            // 任务清单为空视为无效结果，降级模板实例化保证落库有内容
            if (data.getTasks() == null || data.getTasks().isEmpty()) {
                log.warn("AI 预案生成任务清单为空, 降级模板实例化: warningEventId={}", request.getWarningEventId());
                return null;
            }
            return data;
        } catch (Exception e) {
            // AI 服务不可用/超时/熔断：降级模板实例化，不阻塞预案生成主链路
            log.warn("AI 预案生成调用失败, 降级模板实例化: warningEventId={}, cause={}",
                    request.getWarningEventId(), e.getMessage());
            return null;
        }
    }

    /**
     * 组装 AI 预案代理请求（缺省标题/描述由预警上下文兜底拼装）。
     *
     * @param request 预案生成请求
     * @return AI 预案代理请求 DTO
     */
    private PlanAgentRequestDTO buildAgentRequest(PlanGenerateRequest request) {
        PlanAgentRequestDTO dto = new PlanAgentRequestDTO();
        dto.setWarningId(request.getWarningEventId());
        dto.setPipeType(request.getPipeType());
        dto.setWarningLevel(request.getWarningLevel());
        dto.setMetricType(request.getMetricType());
        dto.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle()
                : "管网安全预警处置预案（预警事件 " + request.getWarningEventId() + "）");
        dto.setDescription(StringUtils.hasText(request.getDescription())
                ? request.getDescription()
                : buildFallbackDescription(request));
        return dto;
    }

    /**
     * 缺省描述兜底拼装：合并场景类型与位置描述。
     *
     * @param request 预案生成请求
     * @return 描述文本
     */
    private String buildFallbackDescription(PlanGenerateRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.getScenarioType())) {
            sb.append("场景类型：").append(request.getScenarioType()).append("；");
        }
        if (StringUtils.hasText(request.getLocationDesc())) {
            sb.append("事发位置：").append(request.getLocationDesc()).append("。");
        }
        return sb.length() == 0 ? "管网安全预警事件，需生成应急处置预案。" : sb.toString();
    }
}
