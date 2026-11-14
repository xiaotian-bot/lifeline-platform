package com.tianxin.lifeline.operation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 运维工单服务启动类。
 *
 * <p>职责：运维工单全生命周期状态机管理（0 待派单 / 1 待接单 / 2 处置中 /
 * 3 已完成 / 4 已关闭）、巡检计划管理、工单事件（pipe_order_event）与
 * 处置反馈事件（pipe_disposal_event）Kafka 发布，供预警核销联动。
 * 端口 9007，Nacos 注册名 operation-service。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tianxin.lifeline.api")
@MapperScan("com.tianxin.lifeline.operation.mapper")
@SpringBootApplication
public class OperationApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OperationApplication.class, args);
    }
}
