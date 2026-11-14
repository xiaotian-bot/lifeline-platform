package com.tianxin.lifeline.warning.constant;

/**
 * 预警服务 Redis 键常量与构造工具。
 *
 * <p>三类键：
 * <ul>
 *     <li>预警抑制键：同设备同指标窗口内不重复投递预警事件；</li>
 *     <li>事件幂等键：Kafka 事件 eventId 级去重；</li>
 *     <li>编号幂等键：预警编号 warningNo（event_code）级去重。</li>
 * </ul>
 */
public final class WarningRedisKeys {

    private WarningRedisKeys() {
    }

    /** 预警抑制键前缀（同设备同指标窗口抑制） */
    public static final String SUPPRESS_PREFIX = "lifeline:warning:suppress:";

    /** 事件幂等键前缀（Kafka eventId 去重） */
    public static final String EVENT_IDEMPOTENT_PREFIX = "lifeline:warning:idem:event:";

    /** 编号幂等键前缀（warningNo/event_code 去重） */
    public static final String NO_IDEMPOTENT_PREFIX = "lifeline:warning:idem:no:";

    /** 推送记录幂等键前缀 */
    public static final String PUSH_IDEMPOTENT_PREFIX = "lifeline:warning:idem:push:";

    /**
     * 构建预警抑制键。
     *
     * @param deviceId   设备标识
     * @param metricType 指标类型
     * @return Redis 键
     */
    public static String suppress(String deviceId, String metricType) {
        return SUPPRESS_PREFIX + deviceId + ":" + metricType;
    }

    /**
     * 构建事件幂等键。
     *
     * @param eventId Kafka 事件ID
     * @return Redis 键
     */
    public static String eventIdempotent(String eventId) {
        return EVENT_IDEMPOTENT_PREFIX + eventId;
    }

    /**
     * 构建编号幂等键。
     *
     * @param warningNo 预警编号
     * @return Redis 键
     */
    public static String noIdempotent(String warningNo) {
        return NO_IDEMPOTENT_PREFIX + warningNo;
    }

    /**
     * 构建推送记录幂等键。
     *
     * @param pushId 推送事件ID
     * @return Redis 键
     */
    public static String pushIdempotent(String pushId) {
        return PUSH_IDEMPOTENT_PREFIX + pushId;
    }
}
