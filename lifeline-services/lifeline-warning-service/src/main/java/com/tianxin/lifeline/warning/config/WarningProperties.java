package com.tianxin.lifeline.warning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 预警服务业务配置属性。
 *
 * <p>配置前缀 {@code lifeline.warning}，示例：
 * <pre>
 * lifeline:
 *   warning:
 *     rule-refresh-ms: 60000
 *     suppress-minutes: 10
 *     idempotent-ttl-minutes: 10
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.warning")
public class WarningProperties {

    /** 规则缓存定时刷新间隔（毫秒），默认 60 秒 */
    private long ruleRefreshMs = 60000L;

    /** 同设备同指标预警抑制窗口（分钟），默认 10 分钟 */
    private long suppressMinutes = 10L;

    /** 幂等去重键 TTL（分钟），默认 10 分钟 */
    private long idempotentTtlMinutes = 10L;
}
