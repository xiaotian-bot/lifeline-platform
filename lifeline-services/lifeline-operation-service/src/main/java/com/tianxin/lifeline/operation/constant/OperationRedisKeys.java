package com.tianxin.lifeline.operation.constant;

/**
 * 运维工单服务 Redis 键常量与构造工具。
 *
 * <p>当前仅含预警自动派单幂等键：Kafka 事件 eventId 级去重，
 * 防止同一预警事件重复投递导致重复建单。
 */
public final class OperationRedisKeys {

    private OperationRedisKeys() {
    }

    /** 预警自动派单幂等键前缀（Kafka eventId 去重） */
    public static final String AUTO_DISPATCH_IDEMPOTENT_PREFIX = "lifeline:operation:idem:auto-dispatch:";

    /**
     * 构建预警自动派单幂等键。
     *
     * @param eventId Kafka 事件ID
     * @return Redis 键
     */
    public static String autoDispatchIdempotent(String eventId) {
        return AUTO_DISPATCH_IDEMPOTENT_PREFIX + eventId;
    }
}
