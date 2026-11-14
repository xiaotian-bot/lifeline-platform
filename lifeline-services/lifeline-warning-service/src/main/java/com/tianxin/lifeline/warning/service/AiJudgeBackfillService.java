package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.warning.entity.WarningEvent;

/**
 * AI 研判异步回填服务。
 *
 * <p>预警事件落库后提交异步研判任务：调用 {@link AiJudgeAdapter#judgeDetail}
 * 深度研判，将置信度回填 warning_event.ai_score、研判结论回填
 * warning_event.ai_result，并写 ai_judged_at 作为「已研判」幂等标记。
 *
 * <p>契约：全程异步不阻塞预警主消费链路；AI 失败/超时/降级返回 null 时
 * 维持现状不回填；同一预警已有研判结果（ai_judged_at 非空）时跳过。
 */
public interface AiJudgeBackfillService {

    /**
     * 提交预警事件的异步 AI 研判回填任务。
     *
     * <p>提交动作本身绝不抛异常，任何失败仅记录日志。
     *
     * @param saved 已落库的预警事件（null 或已研判时直接忽略）
     */
    void submitAsyncJudge(WarningEvent saved);
}
