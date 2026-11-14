package com.tianxin.lifeline.construction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 施工监护服务启动类。
 *
 * <p>职责：施工项目/工序管理、项目状态机推进（条件更新防竞态）、
 * 周边管线 PostGIS 空间影响分析、AI 视觉质检（Feign 失败降级人工复核）、
 * 违规事件经 EventEnvelope 信封投递 pipe_evidence_request 触发区块链存证、
 * MinIO 施工影像流式上传（DB 只存对象键）。
 * 端口 9006，Nacos 注册名 construction-service。
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.tianxin.lifeline.api")
@MapperScan("com.tianxin.lifeline.construction.mapper")
@SpringBootApplication
public class ConstructionApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ConstructionApplication.class, args);
    }
}
