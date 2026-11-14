package com.tianxin.lifeline.twin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 数字孪生聚合服务启动类。
 *
 * <p>端口 9010，Nacos 注册名 digital-twin-service，网关路由
 * /api/v1/twin/** → lb://digital-twin-service。
 *
 * <p>纯聚合编排型服务：只读拼装 pipe-asset / monitor / warning 既有能力，
 * 不自建存储（仿真任务状态由 Redis 承载）、不落 PG 业务表，故无 MapperScan。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tianxin.lifeline.api")
@SpringBootApplication
public class TwinApplication {

    /**
     * 启动入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TwinApplication.class, args);
    }
}
