package com.tianxin.lifeline.construction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 施工监护服务业务参数配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.construction")
public class ConstructionProperties {

    /** 人工违规上报幂等去重键 TTL（分钟） */
    private long idempotentTtlMinutes = 1;
}
