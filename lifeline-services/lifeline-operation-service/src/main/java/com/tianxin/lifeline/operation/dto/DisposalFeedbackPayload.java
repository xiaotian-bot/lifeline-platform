package com.tianxin.lifeline.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 处置反馈负载（pipe_disposal_event Topic 的 payload，eventType=DISPOSAL_FEEDBACK）。
 *
 * <p>工单完成（complete）时发布，供 warning-service 消费做预警核销联动。
 * 分区键使用 warningEventId，保证同一预警事件的处置反馈有序。
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
