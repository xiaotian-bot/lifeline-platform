package com.tianxin.lifeline.aigateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 能力调用模板：缓存读取 → 上游转发（分级超时）→ 结构化降级 → 结果回写缓存 → 调用日志。
 *
 * <p>统一降级策略：上游超时/不可达/非2xx/反序列化失败时，不抛 5xx，
 * 返回 HTTP 200 + Result 包装（code=200、data=null、message 说明降级原因），
 * 调用方按「data 是否为空」自行决定退回本地策略，与现有
 * DefaultAiJudgeAdapter 的失败降级契约保持一致。
 *
 * <p>成本决策：不引入 Sentinel 限流熔断组件，以 WebClient 连接超时 +
 * Mono.timeout 分级读超时 + onErrorResume 结构化降级替代。后续如需
 * 限流/熔断，可仅在本模板（唯一切入点）叠加 Sentinel WebFlux 适配器。
 */
@Slf4j
@Component
public class AiCapabilityTemplate {

    /** 降级结果的 message 前缀 */
    public static final String DEGRADED_MESSAGE_PREFIX = "已降级：";

    private final AiCacheService cacheService;
    private final AiInvokeLogger invokeLogger;
    private final ObjectMapper objectMapper;

    /**
     * 构造调用模板。
     *
     * @param cacheService 结果缓存服务
     * @param invokeLogger 调用日志器
     * @param objectMapper JSON 序列化器
     */
    public AiCapabilityTemplate(AiCacheService cacheService,
                                AiInvokeLogger invokeLogger,
                                ObjectMapper objectMapper) {
        this.cacheService = cacheService;
        this.invokeLogger = invokeLogger;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用结果包装：携带降级/缓存命中标记，供 Controller 做后续决策
     * （如 forecast 仅对非降级、非缓存结果落库）。
     *
     * @param result   统一响应体
     * @param degraded 是否上游失败降级
     * @param cacheHit 是否缓存命中
     * @param <RES>    业务数据类型
     */
    public record InvokeOutcome<RES>(Result<RES> result, boolean degraded, boolean cacheHit) {
    }

    /**
     * 调用 AI 能力并返回完整结果包装。
     *
     * @param capability   能力名（如 anomaly-detect/forecast/agent-judge）
     * @param client       上游 WebClient（ml 或 agent）
     * @param uri          上游路径（如 /api/v1/ai/anomaly-detect）
     * @param request      请求体
     * @param responseType 响应反序列化目标类型
     * @param timeoutMs    该能力读超时（毫秒）
     * @param cacheable    是否启用结果缓存
     * @param <REQ>        请求类型
     * @param <RES>        响应类型
     * @return 调用结果包装
     */
    public <REQ, RES> Mono<InvokeOutcome<RES>> invokeRaw(String capability,
                                                         WebClient client,
                                                         String uri,
                                                         REQ request,
                                                         Class<RES> responseType,
                                                         long timeoutMs,
                                                         boolean cacheable) {
        long startMs = System.currentTimeMillis();
        AtomicBoolean degraded = new AtomicBoolean(false);
        AtomicBoolean cacheHit = new AtomicBoolean(false);

        String requestJson = cacheService.toJson(request);
        String cacheKey = cacheable ? cacheService.buildKey(capability, requestJson) : null;

        // 1) 缓存读取：命中直接返回；未命中/异常走上游
        Mono<InvokeOutcome<RES>> fromCache = cacheable
                ? cacheService.get(cacheKey).flatMap(json -> {
                    try {
                        RES data = objectMapper.readValue(json, responseType);
                        cacheHit.set(true);
                        return Mono.just(new InvokeOutcome<>(Result.success(data), false, true));
                    } catch (Exception e) {
                        // 缓存内容损坏：忽略并回源
                        log.warn("AI缓存反序列化失败, 回源上游: capability={}, cause={}", capability, e.getMessage());
                        return Mono.empty();
                    }
                })
                : Mono.empty();

        // 2) 上游转发：分级超时 + 结构化降级
        Mono<InvokeOutcome<RES>> fromUpstream = client.post()
                .uri(uri)
                .header("Content-Type", "application/json")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(responseType)
                // 上游返回空体视为异常，进入降级分支
                .switchIfEmpty(Mono.error(new IllegalStateException("上游返回空响应")))
                .timeout(Duration.ofMillis(timeoutMs))
                // 3) 成功后回写缓存（异常不阻断）
                .flatMap(data -> cacheable
                        ? cacheService.put(cacheKey, toJsonSafe(data)).thenReturn(data)
                        : Mono.just(data))
                .map(data -> new InvokeOutcome<>(Result.success(data), false, false))
                .onErrorResume(e -> {
                    degraded.set(true);
                    log.warn("AI能力调用失败, 返回结构化降级结果: capability={}, cause={}",
                            capability, e.getMessage());
                    return Mono.just(new InvokeOutcome<>(degradedResult(capability, e), true, false));
                });

        return fromCache.switchIfEmpty(fromUpstream)
                // 4) 调用日志（能力/耗时/是否降级/缓存命中）
                .doFinally(signal -> invokeLogger.log(capability,
                        System.currentTimeMillis() - startMs, degraded.get(), cacheHit.get()));
    }

    /**
     * 调用 AI 能力（仅返回统一响应体，忽略降级/缓存标记）。
     *
     * @param capability   能力名
     * @param client       上游 WebClient
     * @param uri          上游路径
     * @param request      请求体
     * @param responseType 响应类型
     * @param timeoutMs    读超时（毫秒）
     * @param cacheable    是否缓存
     * @param <REQ>        请求类型
     * @param <RES>        响应类型
     * @return 统一响应体
     */
    public <REQ, RES> Mono<Result<RES>> invoke(String capability,
                                               WebClient client,
                                               String uri,
                                               REQ request,
                                               Class<RES> responseType,
                                               long timeoutMs,
                                               boolean cacheable) {
        return invokeRaw(capability, client, uri, request, responseType, timeoutMs, cacheable)
                .map(InvokeOutcome::result);
    }

    /**
     * 构造结构化降级结果：HTTP 200 + code=200 + data=null + 明确降级 message。
     *
     * @param capability 能力名
     * @param cause      失败原因
     * @param <RES>      业务数据类型
     * @return 降级响应
     */
    public <RES> Result<RES> degradedResult(String capability, Throwable cause) {
        Result<RES> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        result.setData(null);
        result.setMessage(DEGRADED_MESSAGE_PREFIX + "AI能力[" + capability + "]上游不可用或超时("
                + cause.getClass().getSimpleName() + ")，本次返回空结果");
        return result;
    }

    /**
     * 安全序列化结果对象为 JSON（缓存写入用）。
     *
     * @param data 结果对象
     * @return JSON 字符串
     */
    private String toJsonSafe(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}
