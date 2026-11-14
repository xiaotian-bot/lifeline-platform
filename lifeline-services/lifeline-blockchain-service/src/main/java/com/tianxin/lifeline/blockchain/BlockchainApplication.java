package com.tianxin.lifeline.blockchain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 区块链存证服务启动类。
 *
 * <p>职责：管网 DID 数字身份注册、全环节存证上链（适配器模式：
 * 默认 PG 模拟账本，预留 Hyperledger Fabric 2.5）、存证溯源时间线、
 * 防篡改校验。端口 9008，Nacos 注册名 blockchain-service。
 */
@EnableDiscoveryClient
@MapperScan("com.tianxin.lifeline.blockchain.mapper")
@SpringBootApplication
public class BlockchainApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BlockchainApplication.class, args);
    }
}
