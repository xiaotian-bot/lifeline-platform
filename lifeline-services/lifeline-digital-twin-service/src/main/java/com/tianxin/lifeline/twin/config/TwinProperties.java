package com.tianxin.lifeline.twin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数字孪生聚合服务业务参数（配置前缀 lifeline.twin）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.twin")
public class TwinProperties {

    /** 实时映射单批次上限（超限分页处理），默认 500 */
    private int realtimeBatchSize = 500;

    /** 仿真任务状态 Redis TTL（分钟），默认 30 */
    private long simulationTtlMinutes = 30;
}
