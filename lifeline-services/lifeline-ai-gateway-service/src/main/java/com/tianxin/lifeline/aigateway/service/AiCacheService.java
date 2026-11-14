package com.tianxin.lifeline.aigateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.aigateway.config.AiGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * AI 能力结果缓存服务（Redis，TTL 5 分钟）。
 *
 * <p>Key = 前缀 + 能力名 + 入参 JSON 的 SHA-256 摘要；Value = 结果 JSON。
 * 缓存读写异常一律吞掉（降级为未命中/不写入），绝不阻断主链路。
 */
@Slf4j
@Service
public class AiCacheService {

    /** 缓存操作自身的保护性超时（Redis 异常时快速放弃，避免拖慢 AI 调用） */
    private static final Duration CACHE_OP_TIMEOUT = Duration.ofMillis(500);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 构造缓存服务。
     *
     * @param redisTemplate 响应式 Redis 模板（Boot 自动装配 ReactiveStringRedisTemplate）
     * @param properties    AI 网关配置
     * @param objectMapper  JSON 序列化器
     */
    public AiCacheService(ReactiveStringRedisTemplate redisTemplate,
                          AiGatewayProperties properties,
                          ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建缓存 Key：前缀 + 能力名 + ":" + SHA-256(入参JSON)。
     *
     * @param capability  能力名（如 anomaly-detect）
     * @param requestJson 入参 JSON 字符串
     * @return 缓存 Key
     */
    public String buildKey(String capability, String requestJson) {
        return properties.getCache().getKeyPrefix() + capability + ":" + sha256(requestJson);
    }

    /**
     * 序列化请求对象为 JSON（用于缓存 Key 哈希）。失败时返回空对象 JSON。
     *
     * @param request 请求对象
     * @return JSON 字符串
     */
    public String toJson(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("AI缓存Key序列化失败, 使用空对象哈希: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 读取缓存。未命中/开关关闭/异常均返回 empty（不阻断主链路）。
     *
     * @param key 缓存 Key
     * @return 缓存的结果 JSON
     */
    public Mono<String> get(String key) {
        if (!properties.getCache().isEnabled()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue().get(key)
                .timeout(CACHE_OP_TIMEOUT)
                .onErrorResume(e -> {
                    log.debug("AI缓存读取异常（忽略）: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * 写入缓存（带 TTL）。异常吞掉，不阻断主链路。
     *
     * @param key   缓存 Key
     * @param value 结果 JSON
     * @return 完成信号
     */
    public Mono<Void> put(String key, String value) {
        if (!properties.getCache().isEnabled()) {
            return Mono.empty();
        }
        Duration ttl = Duration.ofSeconds(properties.getCache().getTtlSeconds());
        return redisTemplate.opsForValue().set(key, value, ttl)
                .timeout(CACHE_OP_TIMEOUT)
                .doOnError(e -> log.debug("AI缓存写入异常（忽略）: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(Boolean.FALSE))
                .then();
    }

    /**
     * SHA-256 摘要（十六进制）。
     *
     * @param text 原文
     * @return 摘要字符串
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 为 JDK 必备算法，理论不可达；兜底退化为 hashCode
            return Integer.toHexString(text.hashCode());
        }
    }
}
