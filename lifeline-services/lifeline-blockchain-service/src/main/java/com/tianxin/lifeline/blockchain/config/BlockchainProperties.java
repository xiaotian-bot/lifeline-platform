package com.tianxin.lifeline.blockchain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 区块链存证服务业务配置属性。
 *
 * <p>配置前缀 {@code lifeline.blockchain}，示例：
 * <pre>
 * lifeline:
 *   blockchain:
 *     adapter: pg
 *     idempotent-ttl-minutes: 10
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.blockchain")
public class BlockchainProperties {

    /** 适配器类型：pg=PG模拟账本（默认） / fabric=Hyperledger Fabric 2.5（占位） */
    private String adapter = "pg";

    /** 存证请求事件幂等去重键 TTL（分钟），默认 10 分钟 */
    private long idempotentTtlMinutes = 10L;
}
