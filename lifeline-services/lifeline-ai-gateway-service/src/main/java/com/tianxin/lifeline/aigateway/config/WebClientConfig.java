package com.tianxin.lifeline.aigateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient 配置：分别面向 ml-service 与 agent-service 构建客户端。
 *
 * <p>超时策略（不引入 Sentinel，以超时 + 降级替代熔断）：
 * <ul>
 *     <li>连接超时：统一 3 秒（TCP 建连阶段，上游不可达快速失败）；</li>
 *     <li>读超时：按能力分级（detect 5s / forecast 30s / agent 60s / health 2s），
 *         在 {@code AiCapabilityTemplate} 中通过 Mono.timeout 实现，
 *         超时后统一 onErrorResume 返回结构化降级结果。</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    /** TCP 连接超时（毫秒） */
    private static final int CONNECT_TIMEOUT_MS = 3000;

    /**
     * ml-service 上游客户端（异常检测/趋势预报）。
     *
     * @param properties AI 网关配置
     * @return WebClient
     */
    @Bean
    public WebClient mlWebClient(AiGatewayProperties properties) {
        return buildClient(properties.getMlService().getUrl());
    }

    /**
     * agent-service 上游客户端（研判/预案/问答/工作流）。
     *
     * @param properties AI 网关配置
     * @return WebClient
     */
    @Bean
    public WebClient agentWebClient(AiGatewayProperties properties) {
        return buildClient(properties.getAgentService().getUrl());
    }

    /**
     * 连通性自检客户端（无固定 baseUrl，按请求绝对地址调用）。
     *
     * @return WebClient
     */
    @Bean
    public WebClient healthWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)))
                // 自检请求体不会很大，限制内存即可
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
    }

    /**
     * 构建带连接超时的 WebClient。
     *
     * @param baseUrl 上游基础地址
     * @return WebClient
     */
    private WebClient buildClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS);
        // 不设全局 responseTimeout：各能力按 Mono.timeout 分级控制，
        // 便于 detect(5s)/forecast(30s)/agent(60s) 差异化超时
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // AI 报告正文可能较大（reportContent 等），放宽内存缓冲至 4MB
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}
