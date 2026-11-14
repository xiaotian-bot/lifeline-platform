package com.tianxin.lifeline.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单事件负载（pipe_order_event Topic 的 payload）。
 *
 * <p>工单创建与每次状态变更均发布该负载，eventType 区分
 * ORDER_CREATED / ORDER_ACCEPTED / ORDER_COMPLETED / ORDER_CLOSED。
 */
@Data
public class OrderEventPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long orderId;

    /** 工单编号 */
    private String orderCode;

    /** 工单类型：REPAIR / INSPECT / MAINTAIN / EMERGENCY */
    private String orderType;

    /** 变更后工单状态数值：0-4 */
    private Integer status;

    /** 优先级：1 最高 - 4 最低 */
    private Integer priority;

    /** 关联预警事件ID（无关联为 null） */
    private Long warningEventId;

    /** 责任人标识 */
    private String assignee;

    /** 工单来源：WARNING / PLAN / MANUAL / AI */
    private String source;
}
