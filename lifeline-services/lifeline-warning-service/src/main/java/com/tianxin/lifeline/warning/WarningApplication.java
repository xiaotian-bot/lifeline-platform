package com.tianxin.lifeline.warning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 预警预报服务启动类。
 *
 * <p>职责：传感数据规则判定（pipe_sensor_raw → pipe_warning_event）、
 * 预警事件幂等落库与四级分级推送、AI 深研预留、预报报告与预警规则管理。
 * 端口 9004，Nacos 注册名 warning-service。
 */
@EnableScheduling
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tianxin.lifeline.api")
@MapperScan("com.tianxin.lifeline.warning.mapper")
@SpringBootApplication
public class WarningApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(WarningApplication.class, args);
    }
}
