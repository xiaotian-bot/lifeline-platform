package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.api.ai.dto.AgentJudgeResultDTO;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.entity.WarningEvent;

import java.math.BigDecimal;

/**
 * AI 深研适配器接口（P8 阶段接入点）。
 *
 * <p>预警事件落库前调用，对触发样本做 AI 异常检测并产出 0~1 的异常评分。
 * 契约约定：实现必须保证「失败可降级」——AI 服务不可用、超时或返回异常时
 * 返回 null，表示退回纯规则判定，不阻塞预警主链路。
 */
public interface AiJudgeAdapter {

    /**
     * 对预警负载做 AI 异常评分。
     *
     * @param payload 预警事件负载
     * @return 异常评分（0~1），AI 不可用或降级时返回 null
     */
    BigDecimal judge(WarningEventPayload payload);

    /**
     * 对已落库预警事件做 AI 智能研判代理深度研判（根因分析/等级复核/
     * 影响范围/趋势预测/处置优先级）。
     *
     * <p>同样遵循「失败可降级」契约：AI 服务不可用、超时、返回失败或
     * 结果无效时返回 null，调用方维持现状不回填，绝不影响预警主流程。
     *
     * @param event 已落库的预警事件
     * @return 研判结果，AI 不可用或降级时返回 null
     */
    AgentJudgeResultDTO judgeDetail(WarningEvent event);
}
