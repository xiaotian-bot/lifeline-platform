package com.tianxin.lifeline.twin.constant;

/**
 * 数字孪生聚合服务 Redis 键常量。
 *
 * <p>本服务自建键一律以 {@code lifeline:twin:} 为前缀，避免与既有键空间冲突；
 * 对 monitor-data-service 写入的实时缓存键 {@code rt:{deviceId}} 仅只读引用。
 */
public final class TwinRedisKeys {

    private TwinRedisKeys() {
    }

    /** 本服务键统一前缀 */
    public static final String PREFIX = "lifeline:twin:";

    /**
     * monitor-data-service 实时缓存键前缀（HASH，TTL 10 分钟，本服务只读）。
     * 完整键形如 {@code rt:DEV-GAS-001}，字段含 pipe_type/sensor_type/area_id/ts/
     * pressure/temperature/flow/vibration/corrosion/leakage/status。
     */
    public static final String MONITOR_RT_PREFIX = "rt:";

    /** 预警定位结果缓存键前缀（String，JSON，TTL 1 分钟），完整键 {@code lifeline:twin:warning-loc:{scope}} */
    public static final String WARNING_LOCATION_PREFIX = PREFIX + "warning-loc:";

    /** 仿真任务状态键前缀（HASH，TTL 30 分钟），完整键 {@code lifeline:twin:sim:{taskId}} */
    public static final String SIMULATION_PREFIX = PREFIX + "sim:";

    /**
     * 拼装预警定位缓存键。
     *
     * @param scope 作用域标识（预警ID或 pipeId 维度）
     * @return 完整 Redis 键
     */
    public static String warningLocationKey(String scope) {
        return WARNING_LOCATION_PREFIX + scope;
    }

    /**
     * 拼装仿真任务状态键。
     *
     * @param taskId 仿真任务ID
     * @return 完整 Redis 键
     */
    public static String simulationKey(String taskId) {
        return SIMULATION_PREFIX + taskId;
    }
}
