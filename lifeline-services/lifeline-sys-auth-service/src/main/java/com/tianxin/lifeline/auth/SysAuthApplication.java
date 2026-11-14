package com.tianxin.lifeline.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 系统用户权限服务启动类。
 *
 * <p>职责：统一用户管理、JWT 双令牌身份认证、角色/权限管理、
 * 菜单权限树、操作审计日志。端口 9001。
 */
@EnableAsync
@EnableDiscoveryClient
@MapperScan("com.tianxin.lifeline.auth.mapper")
@SpringBootApplication
public class SysAuthApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SysAuthApplication.class, args);
    }
}
