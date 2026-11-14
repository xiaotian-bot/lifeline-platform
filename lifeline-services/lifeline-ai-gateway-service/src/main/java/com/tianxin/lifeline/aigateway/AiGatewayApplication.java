package com.tianxin.lifeline.aigateway;

import com.tianxin.lifeline.aigateway.config.AiGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI能力网关服务启动类（P8）。
 *
 * <p>WebFlux 响应式栈，端口 9009，Nacos 注册名 ai-gateway-service。
 * 统一转发 ml-service（异常检测/趋势预报）与 agent-service
 * （研判/预案/问答/工作流）的 AI 能力，并提供结果缓存、超时降级、
 * 批量推理与 forecast 报告直写 PG。
 */
@SpringBootApplication
@EnableConfigurationProperties(AiGatewayProperties.class)
public class AiGatewayApplication {

    /**
     * 启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
    }
}
