package com.tianxin.lifeline.aigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 能力网关配置项（前缀 lifeline.ai）。
 *
 * <p>包含上游 Python 服务地址、各能力分级超时、结果缓存参数。
 */
@Data
@ConfigurationProperties(prefix = "lifeline.ai")
public class AiGatewayProperties {

    /** ml-service（FastAPI:8000）配置 */
    private MlService mlService = new MlService();

    /** agent-service（LangGraph:8001）配置 */
    private AgentService agentService = new AgentService();

    /** 各能力超时配置（毫秒） */
    private Timeout timeout = new Timeout();

    /** 结果缓存配置 */
    private Cache cache = new Cache();

    /**
     * ml-service 上游配置。
     */
    @Data
    public static class MlService {
        /** 上游地址 */
        private String url = "http://localhost:8000";
    }

    /**
     * agent-service 上游配置。
     */
    @Data
    public static class AgentService {
        /** 上游地址 */
        private String url = "http://localhost:8001";
    }

    /**
     * 各能力分级超时配置。
     *
     * <p>成本决策说明：本服务不引入 Sentinel 限流熔断组件，
     * 以 WebClient 连接超时 + Mono.timeout 分级读超时 + onErrorResume
     * 结构化降级替代熔断能力。后续若需限流/熔断，可在各能力的
     * 调用链上叠加 Sentinel WebFlux 适配器（AiCapabilityTemplate 为唯一切入点）。
     */
    @Data
    public static class Timeout {
        /** 时序异常检测超时（毫秒） */
        private long detectMs = 5000L;
        /** 趋势预报超时（毫秒） */
        private long forecastMs = 30000L;
        /** Agent 类能力（judge/plan/chat/workflow）超时（毫秒） */
        private long agentMs = 60000L;
        /** 上游连通性自检超时（毫秒） */
        private long healthMs = 2000L;
    }

    /**
     * Redis 结果缓存配置。
     */
    @Data
    public static class Cache {
        /** 缓存总开关 */
        private boolean enabled = true;
        /** 缓存 TTL（秒），默认 5 分钟 */
        private long ttlSeconds = 300L;
        /** Key 前缀 */
        private String keyPrefix = "lifeline:ai:cache:";
    }
}
