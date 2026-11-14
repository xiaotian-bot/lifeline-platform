package com.tianxin.lifeline.plan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 预案处置服务启动类。
 *
 * <p>职责：应急预案模板管理、AI 智能预案生成（Feign 失败降级为按模板
 * content 实例化通用预案）、预案执行状态机与任务推进、工单派单联动、
 * 消费 pipe_disposal_event 处置反馈回写预案任务状态。
 * 端口 9005，Nacos 注册名 plan-service。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tianxin.lifeline.api")
@MapperScan("com.tianxin.lifeline.plan.mapper")
@SpringBootApplication
public class PlanApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PlanApplication.class, args);
    }
}
