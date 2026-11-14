package com.tianxin.lifeline.aigateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 能力调用日志：每次请求记录一行 JSON（能力/耗时/是否降级/缓存命中）。
 *
 * <p>采用 JSON 行格式输出到常规日志通道，便于 ELK/Loki 等采集端
 * 直接按结构化字段检索，无需额外 MDC 配置。
 */
@Slf4j
@Component
public class AiInvokeLogger {

    private final ObjectMapper objectMapper;

    /**
     * 构造日志器。
     *
     * @param objectMapper JSON 序列化器
     */
    public AiInvokeLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一次 AI 能力调用。
     *
     * @param capability 能力名（如 anomaly-detect/forecast/agent-judge）
     * @param costMs     调用总耗时（毫秒）
     * @param degraded   是否降级（上游超时/不可达）
     * @param cacheHit   是否缓存命中
     */
    public void log(String capability, long costMs, boolean degraded, boolean cacheHit) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("event", "ai_invoke");
        fields.put("capability", capability);
        fields.put("costMs", costMs);
        fields.put("degraded", degraded);
        fields.put("cacheHit", cacheHit);
        fields.put("ts", System.currentTimeMillis());
        try {
            log.info(objectMapper.writeValueAsString(fields));
        } catch (JsonProcessingException e) {
            log.info("ai_invoke capability={} costMs={} degraded={} cacheHit={}",
                    capability, costMs, degraded, cacheHit);
        }
    }
}
