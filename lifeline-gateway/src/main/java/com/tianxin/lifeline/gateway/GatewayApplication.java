package com.tianxin.lifeline.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台统一API网关启动类。
 *
 * <p>职责：统一入口路由转发、全局JWT鉴权、白名单放行。
 * 端口：8080。
 */
@SpringBootApplication
public class GatewayApplication {

    /**
     * 网关启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
