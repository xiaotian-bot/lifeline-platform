package com.tianxin.lifeline.operation.constant;

/**
 * 运维工单 Kafka 事件类型常量。
 *
 * <p>工单创建与状态变更统一发布至 {@code TopicConstants.PIPE_ORDER_EVENT}；
 * 工单完成时额外发布处置反馈事件至 {@code TopicConstants.PIPE_DISPOSAL_EVENT}，
 * 供 warning-service 核销联动消费。
 */
public final class OrderEventType {

    private OrderEventType() {
    }

    /* ==================== pipe_order_event 事件类型 ==================== */

    /** 工单创建 */
    public static final String ORDER_CREATED = "ORDER_CREATED";

    /** 工单接单（待接单 → 处置中） */
    public static final String ORDER_ACCEPTED = "ORDER_ACCEPTED";

    /** 工单完成（处置中 → 已完成） */
    public static final String ORDER_COMPLETED = "ORDER_COMPLETED";

    /** 工单关闭（→ 已关闭） */
    public static final String ORDER_CLOSED = "ORDER_CLOSED";

    /* ==================== pipe_disposal_event 事件类型 ==================== */

    /** 处置反馈（工单完成回传，供预警核销联动） */
    public static final String DISPOSAL_FEEDBACK = "DISPOSAL_FEEDBACK";

    /** 事件信封 source 标识（本服务名） */
    public static final String EVENT_SOURCE = "operation-service";
}
