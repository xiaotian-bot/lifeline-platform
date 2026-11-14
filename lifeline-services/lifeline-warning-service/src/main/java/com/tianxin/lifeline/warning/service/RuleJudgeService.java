package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.warning.dto.SensorRawMessage;

/**
 * 规则判定服务。
 *
 * <p>规则判定桥核心：对单条传感数据按 warning_rule 启用规则做越限匹配，
 * 命中并经抑制窗口校验后，以 EventEnvelope 信封投递 pipe_warning_event。
 */
public interface RuleJudgeService {

    /**
     * 对单条传感数据做规则越限判定。
     *
     * @param message 传感数据
     */
    void judge(SensorRawMessage message);
}
