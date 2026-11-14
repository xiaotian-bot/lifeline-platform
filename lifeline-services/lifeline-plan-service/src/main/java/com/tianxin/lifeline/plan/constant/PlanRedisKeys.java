package com.tianxin.lifeline.plan.constant;

/**
 * 预案服务 Redis 键常量与构造工具。
 *
 * <p>处置反馈幂等键：Kafka 事件 eventId 级 SETNX 去重，
 * 防止 pipe_disposal_event 重复投递导致任务状态重复回写。
 */
public final class PlanRedisKeys {

    private PlanRedisKeys() {
    }

    /** 处置反馈幂等键前缀（Kafka eventId 去重） */
    public static final String DISPOSAL_IDEMPOTENT_PREFIX = "lifeline:plan:idem:disposal:";

    /**
     * 构建处置反馈幂等键。
     *
     * @param eventId Kafka 事件ID
     * @return Redis 键
     */
    public static String disposalIdempotent(String eventId) {
        return DISPOSAL_IDEMPOTENT_PREFIX + eventId;
    }
}
