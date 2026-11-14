package com.tianxin.lifeline.plan.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 预案处置服务业务配置属性。
 *
 * <p>配置前缀 {@code lifeline.plan}，示例：
 * <pre>
 * lifeline:
 *   plan:
 *     idempotent-ttl-minutes: 10
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.plan")
public class PlanProperties {

    /** 处置反馈事件幂等去重键 TTL（分钟），默认 10 分钟 */
    private long idempotentTtlMinutes = 10L;
}
