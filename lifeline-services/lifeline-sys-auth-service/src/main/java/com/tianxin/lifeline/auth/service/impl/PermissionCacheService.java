package com.tianxin.lifeline.auth.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 权限 Redis 缓存。
 *
 * <p>登录成功后将用户权限标识集合写入 Redis，
 * Key 形如 {@code auth:perm:{userId}}，TTL 30 分钟，
 * 用于减少高频鉴权查询对数据库的压力。
 */
@Slf4j
@Component
public class PermissionCacheService {

    /** 缓存 Key 前缀 */
    public static final String CACHE_KEY_PREFIX = "auth:perm:";

    /** 缓存有效期：30 分钟 */
    public static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /** Redis 字符串模板 */
    private final StringRedisTemplate redisTemplate;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造缓存服务。
     *
     * @param redisTemplate Redis 字符串模板
     */
    public PermissionCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 写入用户权限缓存。
     *
     * @param userId      用户ID
     * @param permissions 权限标识集合
     */
    public void cache(Long userId, Set<String> permissions) {
        try {
            redisTemplate.opsForValue().set(buildKey(userId),
                    objectMapper.writeValueAsString(permissions), CACHE_TTL);
        } catch (Exception e) {
            // 缓存失败不影响主流程
            log.warn("写入权限缓存失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * 读取用户权限缓存。
     *
     * @param userId 用户ID
     * @return 权限集合，未命中返回空
     */
    public Optional<Set<String>> get(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(buildKey(userId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, new TypeReference<Set<String>>() {
            }));
        } catch (Exception e) {
            log.warn("读取权限缓存失败 userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 清空全部权限缓存（权限变更后调用）。
     */
    public void evictAll() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清空权限缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 构建缓存 Key。
     *
     * @param userId 用户ID
     * @return 缓存 Key
     */
    private String buildKey(Long userId) {
        return CACHE_KEY_PREFIX + userId;
    }
}
