package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 处置反馈负载（pipe_disposal_event Topic 的 payload，eventType=DISPOSAL_FEEDBACK）。
 *
 * <p>与 operation-service 的 DisposalFeedbackPayload 结构对齐
 * （工单完成时发布），本服务消费后按 warningEventId 关联回写
 * 预案任务状态。字段契约以 common-mq EventEnvelope 反序列化为准，
 * 未知字段自动忽略。
 */
@Data
public class DisposalFeedbackPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long orderId;

    /** 工单编号 */
    private String orderCode;

    /** 关联预警事件ID（分区键，无关联为 null） */
    private Long warningEventId;

    /** 处置结果描述 */
    private String result;
}
