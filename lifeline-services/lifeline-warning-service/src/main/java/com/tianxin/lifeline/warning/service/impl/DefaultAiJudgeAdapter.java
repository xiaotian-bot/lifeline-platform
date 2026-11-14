package com.tianxin.lifeline.warning.service.impl;

import com.tianxin.lifeline.api.ai.AiGatewayFeignClient;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeResultDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyResultDTO;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import com.tianxin.lifeline.warning.service.AiJudgeAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * AI 深研适配器默认实现（失败降级）。
 *
 * <p>当前（P7）以触发值构造单点时序调用 AI 能力网关的时序异常检测
 * （{@link AiGatewayFeignClient#detectAnomaly}），取异常评分写入
 * warning_event.ai_score。
 *
 * <p>P8 阶段接入真实深度研判后，可替换本实现为基于滑动窗口时序序列的
 * 多模型融合检测；本实现保留「失败即降级」契约：AI 服务不可用、
 * 调用异常或返回失败时返回 null，预警主链路退回纯规则判定，不受影响。
 */
@Slf4j
@Component
public class DefaultAiJudgeAdapter implements AiJudgeAdapter {

    /** AI 能力网关 Feign 客户端 */
    private final AiGatewayFeignClient aiGatewayFeignClient;

    /**
     * 构造适配器。
     *
     * @param aiGatewayFeignClient AI 能力网关 Feign 客户端
     */
    public DefaultAiJudgeAdapter(AiGatewayFeignClient aiGatewayFeignClient) {
        this.aiGatewayFeignClient = aiGatewayFeignClient;
    }

    /**
     * 对预警负载做 AI 异常评分（失败降级返回 null）。
     *
     * @param payload 预警事件负载
     * @return 异常评分（0~1），AI 不可用或降级时返回 null
     */
    @Override
    public BigDecimal judge(WarningEventPayload payload) {
        if (payload == null || payload.getTriggerValue() == null) {
            return null;
        }
        try {
            AnomalyRequestDTO request = new AnomalyRequestDTO();
            request.setDeviceId(payload.getDeviceId());
            request.setPipeType(payload.getPipeType());
            request.setMetricType(payload.getMetricType());
            request.setSeries(Collections.singletonList(payload.getTriggerValue()));
            request.setStartTime(payload.getWarningTime() == null
                    ? System.currentTimeMillis()
                    : java.sql.Timestamp.valueOf(payload.getWarningTime()).getTime());

            Result<AnomalyResultDTO> result = aiGatewayFeignClient.detectAnomaly(request);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("AI 异常检测无有效结果, 降级纯规则判定: warningNo={}", payload.getWarningNo());
                return null;
            }
            Double score = result.getData().getAnomalyScore();
            return score == null ? null : BigDecimal.valueOf(score);
        } catch (Exception e) {
            // AI 服务不可用/超时/熔断：降级纯规则判定，不阻塞预警主链路
            log.warn("AI 异常检测调用失败, 降级纯规则判定: warningNo={}, cause={}",
                    payload.getWarningNo(), e.getMessage());
            return null;
        }
    }

    /**
     * 对已落库预警事件做 AI 智能研判代理深度研判（失败降级返回 null）。
     *
     * <p>调用 AI 能力网关 {@link AiGatewayFeignClient#judgeWarning}
     * （转发 agent-service /agent/judge），产出根因分析/等级复核意见/
     * 影响范围/趋势预测/处置优先级与置信度。
     *
     * @param event 已落库的预警事件
     * @return 研判结果，AI 不可用/超时/返回失败时返回 null（维持现状不回填）
     */
    @Override
    public AgentJudgeResultDTO judgeDetail(WarningEvent event) {
        if (event == null || event.getId() == null) {
            return null;
        }
        try {
            AgentJudgeRequestDTO request = new AgentJudgeRequestDTO();
            request.setWarningId(event.getId());
            request.setPipeType(event.getPipeType());
            request.setWarningLevel(event.getWarningLevel());
            request.setMetricType(event.getMetricType());
            request.setTriggerValue(event.getTriggerValue());
            request.setThreshold(event.getThreshold());
            request.setTitle(event.getTitle());
            request.setDescription(event.getDescription());

            Result<AgentJudgeResultDTO> result = aiGatewayFeignClient.judgeWarning(request);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("AI 研判代理无有效结果, 维持现状不回填: warningId={}, eventCode={}",
                        event.getId(), event.getEventCode());
                return null;
            }
            return result.getData();
        } catch (Exception e) {
            // AI 服务不可用/超时/熔断：降级维持现状，绝不影响预警主流程
            log.warn("AI 研判代理调用失败, 维持现状不回填: warningId={}, eventCode={}, cause={}",
                    event.getId(), event.getEventCode(), e.getMessage());
            return null;
        }
    }
}
