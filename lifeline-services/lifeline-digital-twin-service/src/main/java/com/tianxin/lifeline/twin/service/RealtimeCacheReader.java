package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.twin.constant.TwinRedisKeys;
import com.tianxin.lifeline.twin.dto.DeviceRealtimeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * monitor-data-service 实时缓存（{@code rt:{deviceId}} HASH）只读读取器。
 *
 * <p>批量读取使用 Redis Pipeline（SessionCallback 攒批），减少网络往返；
 * 单键不存在或转换失败仅跳过，不中断整批。
 */
@Slf4j
@Component
public class RealtimeCacheReader {

    /** Redis 字符串模板（只读，键序列化与 monitor 写入端保持一致的 UTF-8） */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造读取器。
     *
     * @param stringRedisTemplate Redis 字符串模板
     */
    public RealtimeCacheReader(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Pipeline 批量读取设备实时缓存。
     *
     * @param deviceIds 设备编码列表（调用方保证不超过单批上限）
     * @return 与入参等长的结果列表，未命中或解析失败的元素为 null
     */
    public List<DeviceRealtimeVO> readBatchPipelined(List<String> deviceIds) {
        List<Object> rawList = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(RedisOperations<K, V> operations) {
                for (String deviceId : deviceIds) {
                    operations.opsForHash().entries((K) (TwinRedisKeys.MONITOR_RT_PREFIX + deviceId));
                }
                // Pipeline 模式下返回值由 executePipelined 统一收集
                return null;
            }
        });
        List<DeviceRealtimeVO> result = new ArrayList<>(deviceIds.size());
        for (int i = 0; i < deviceIds.size(); i++) {
            Object raw = i < rawList.size() ? rawList.get(i) : null;
            result.add(convert(deviceIds.get(i), raw));
        }
        return result;
    }

    /**
     * 读取单个设备实时缓存。
     *
     * @param deviceId 设备编码
     * @return 实时状态，缓存不存在或读取失败返回 null
     */
    public DeviceRealtimeVO readOne(String deviceId) {
        Map<Object, Object> entries;
        try {
            entries = stringRedisTemplate.opsForHash().entries(TwinRedisKeys.MONITOR_RT_PREFIX + deviceId);
        } catch (Exception e) {
            log.warn("Redis实时缓存读取失败: deviceId={}", deviceId, e);
            return null;
        }
        return convert(deviceId, entries);
    }

    /**
     * 将 HASH 条目转换为设备实时状态视图。
     *
     * @param deviceId 设备编码
     * @param raw      Pipeline/单键读取的原始 HASH 条目
     * @return 实时状态视图，条目为空返回 null
     */
    private DeviceRealtimeVO convert(String deviceId, Object raw) {
        if (!(raw instanceof Map<?, ?> entries) || entries.isEmpty()) {
            return null;
        }
        try {
            DeviceRealtimeVO vo = new DeviceRealtimeVO();
            vo.setDeviceId(deviceId);
            vo.setPipeType(asString(entries.get("pipe_type")));
            vo.setSensorType(asString(entries.get("sensor_type")));
            vo.setAreaId(asString(entries.get("area_id")));
            vo.setTs(asLong(entries.get("ts")));
            vo.setPressure(asDouble(entries.get("pressure")));
            vo.setTemperature(asDouble(entries.get("temperature")));
            vo.setFlow(asDouble(entries.get("flow")));
            vo.setVibration(asDouble(entries.get("vibration")));
            vo.setCorrosion(asDouble(entries.get("corrosion")));
            vo.setLeakage(asDouble(entries.get("leakage")));
            vo.setStatus(asInteger(entries.get("status")));
            return vo;
        } catch (Exception e) {
            log.warn("实时缓存HASH解析失败, 跳过: deviceId={}", deviceId, e);
            return null;
        }
    }

    /** 对象转字符串（null 安全） */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 对象转 Long（null 与格式异常安全） */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 对象转 Double（null 与格式异常安全） */
    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 对象转 Integer（null 与格式异常安全） */
    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
