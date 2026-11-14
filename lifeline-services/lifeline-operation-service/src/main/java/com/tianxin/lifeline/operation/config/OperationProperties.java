package com.tianxin.lifeline.operation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 运维工单服务业务配置属性。
 *
 * <p>配置前缀 {@code lifeline.operation}，示例：
 * <pre>
 * lifeline:
 *   operation:
 *     auto-dispatch:
 *       enabled: true
 *       idempotent-ttl-minutes: 10
 *       max-age-minutes: 30
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.operation")
public class OperationProperties {

    /** 预警自动派单配置 */
    private AutoDispatch autoDispatch = new AutoDispatch();

    /**
     * 预警→工单自动派单配置项。
     */
    @Data
    public static class AutoDispatch {

        /** 是否启用预警自动派单（橙色及以上等级预警自动创建应急工单），默认启用 */
        private boolean enabled = true;

        /** 幂等去重键 TTL（分钟），默认 10 分钟 */
        private long idempotentTtlMinutes = 10L;

        /** 预警事件时效阈值（分钟）：预警时间早于当前超过该值的事件跳过派单，
         * 防新消费组 earliest 回放历史预警批量建过期工单；设为 0 关闭时效过滤，默认 30 */
        private long maxAgeMinutes = 30L;
    }
}
