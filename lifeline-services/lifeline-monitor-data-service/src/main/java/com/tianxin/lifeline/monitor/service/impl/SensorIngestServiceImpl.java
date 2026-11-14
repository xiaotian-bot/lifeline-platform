package com.tianxin.lifeline.monitor.service.impl;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.monitor.dto.SensorRawMessage;
import com.tianxin.lifeline.monitor.service.SensorIngestService;
import com.tianxin.lifeline.monitor.ws.MonitorRealtimePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感数据接入服务实现。
 *
 * <p>三路写入职责划分：
 * <ul>
 *     <li>TDengine：全量数据经 {@link TDengineBatchWriter} 攒批写入时序库；</li>
 *     <li>Redis：HASH 结构 {@code rt:{deviceId}} 维护设备最新指标，TTL 10 分钟；</li>
 *     <li>PostgreSQL：按 device_code 回写 pipe_device.status。</li>
 * </ul>
 *
 * <p><b>deviceId 语义决策</b>：本模块全链路（接口、缓存、TDengine 标签）
 * 的 deviceId 一律采用「设备编码字符串」（如 DEV-GAS-001，与 Mock 上报、
 * TDengine 标签 device_id 对齐）；PG 回写按 pipe_device.device_code 匹配，
 * 不使用数值主键 id。
 *
 * <p><b>pipeType 归一化决策</b>：以 common-core 的 {@link PipeType} 枚举为准，
 * 消费端将上报值（大小写不限，如 water/Water）统一归一化为枚举编码
 * （WATER/HEAT/GAS/SEWAGE/HAZARD）后写入；无法识别的值原样保留并告警，
 * 避免静默丢数。
 */
@Slf4j
@Service
public class SensorIngestServiceImpl implements SensorIngestService {

    /** Redis 实时缓存 key 前缀 */
    private static final String REDIS_KEY_PREFIX = "rt:";

    /** Redis 实时缓存 TTL（10 分钟） */
    private static final Duration REDIS_TTL = Duration.ofMinutes(10);

    /** PG 设备状态回写 SQL（以设备编码匹配，仅更新未删除记录） */
    private static final String UPDATE_DEVICE_STATUS_SQL =
            "UPDATE pipe_device SET status = ?, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE device_code = ? AND deleted = 0";

    /** TDengine 攒批写入器 */
    private final TDengineBatchWriter tdengineBatchWriter;

    /** Redis 操作模板（纯字符串键值） */
    private final StringRedisTemplate stringRedisTemplate;

    /** PostgreSQL 主 JdbcTemplate（@Primary，见 TDengineConfig） */
    private final JdbcTemplate jdbcTemplate;

    /** 实时推送发布端（Redis Pub/Sub，发布失败仅日志不影响主链路，P10-3） */
    private final MonitorRealtimePublisher realtimePublisher;

    /**
     * 构造接入服务。
     *
     * @param tdengineBatchWriter TDengine 攒批写入器
     * @param stringRedisTemplate Redis 操作模板
     * @param jdbcTemplate        PostgreSQL 主 JdbcTemplate
     * @param realtimePublisher   实时推送发布端
     */
    public SensorIngestServiceImpl(TDengineBatchWriter tdengineBatchWriter,
                                   StringRedisTemplate stringRedisTemplate,
                                   JdbcTemplate jdbcTemplate,
                                   MonitorRealtimePublisher realtimePublisher) {
        this.tdengineBatchWriter = tdengineBatchWriter;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.realtimePublisher = realtimePublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ingest(SensorRawMessage message) {
        normalize(message);
        writeRedisCache(message);
        writeBackDeviceStatus(message);
        realtimePublisher.publish(message);
        tdengineBatchWriter.submit(List.of(message));
    }

    /**
     * {@inheritDoc}
     *
     * <p>批量模式下（压测单消息 100 条），Redis 与 PG 仅按设备取最后一条回写，
     * 避免同一设备重复写入带来的无谓开销。
     */
    @Override
    public void ingestBatch(List<SensorRawMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        messages.forEach(this::normalize);
        // 按设备编码聚合，保留各设备最新一条用于 Redis/PG 回写
        Map<String, SensorRawMessage> latestByDevice = new LinkedHashMap<>();
        for (SensorRawMessage message : messages) {
            latestByDevice.put(message.getDeviceId(), message);
        }
        latestByDevice.values().forEach(message -> {
            writeRedisCache(message);
            writeBackDeviceStatus(message);
            realtimePublisher.publish(message);
        });
        tdengineBatchWriter.submit(messages);
    }

    /**
     * pipeType 归一化：统一为 {@link PipeType} 枚举编码（大写）。
     *
     * @param message 传感数据（就地修改 pipeType 字段）
     */
    private void normalize(SensorRawMessage message) {
        String raw = message.getPipeType();
        if (raw == null || raw.isBlank()) {
            return;
        }
        PipeType type = PipeType.fromCode(raw.trim());
        if (type == null) {
            log.warn("未知管网类型, 原样保留: deviceId={}, pipeType={}", message.getDeviceId(), raw);
            return;
        }
        message.setPipeType(type.getCode());
    }

    /**
     * 刷新 Redis 实时缓存（HASH 结构，含全部最新指标与采集时间）。
     *
     * <p>Redis 写入失败仅记录错误日志，不影响 TDengine 主链路。
     *
     * @param message 传感数据
     */
    private void writeRedisCache(SensorRawMessage message) {
        try {
            String key = REDIS_KEY_PREFIX + message.getDeviceId();
            Map<String, String> fields = new HashMap<>(16);
            putIfNotNull(fields, "pipe_type", message.getPipeType());
            putIfNotNull(fields, "sensor_type", message.getSensorType());
            putIfNotNull(fields, "area_id", message.getAreaId());
            fields.put("ts", String.valueOf(message.getTs()));
            putIfNotNull(fields, "pressure", message.getPressure());
            putIfNotNull(fields, "temperature", message.getTemperature());
            putIfNotNull(fields, "flow", message.getFlow());
            putIfNotNull(fields, "vibration", message.getVibration());
            putIfNotNull(fields, "corrosion", message.getCorrosion());
            putIfNotNull(fields, "leakage", message.getLeakage());
            putIfNotNull(fields, "status", message.getStatus());
            stringRedisTemplate.opsForHash().putAll(key, fields);
            stringRedisTemplate.expire(key, REDIS_TTL);
        } catch (Exception e) {
            log.error("Redis实时缓存写入失败: deviceId={}", message.getDeviceId(), e);
        }
    }

    /**
     * 回写 PostgreSQL 设备状态。
     *
     * <p><b>状态映射决策</b>：上报契约 0 正常 / 1 异常 / 2 报警，而
     * pipe_device 种子数据默认 status=1（正常/在线），PG 侧无正式状态字典；
     * 故映射为 0→1（正常）、1→2（异常）、2→3（报警），其他值跳过。
     * 该映射为 MVP 约定，若资产域后续定义正式字典需同步调整。
     *
     * @param message 传感数据
     */
    private void writeBackDeviceStatus(SensorRawMessage message) {
        Integer deviceStatus = mapDeviceStatus(message.getStatus());
        if (deviceStatus == null) {
            return;
        }
        try {
            jdbcTemplate.update(UPDATE_DEVICE_STATUS_SQL, deviceStatus, message.getDeviceId());
        } catch (Exception e) {
            log.error("PG设备状态回写失败: deviceId={}", message.getDeviceId(), e);
        }
    }

    /**
     * 上报状态 → pipe_device.status 映射。
     *
     * @param sensorStatus 上报状态（0 正常 / 1 异常 / 2 报警）
     * @return PG 设备状态值，null 表示不回写
     */
    private Integer mapDeviceStatus(Integer sensorStatus) {
        if (sensorStatus == null) {
            return null;
        }
        return switch (sensorStatus) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> null;
        };
    }

    /**
     * 非空值转字符串放入 Map。
     *
     * @param fields 目标 Map
     * @param key    字段名
     * @param value  字段值（null 时跳过）
     */
    private void putIfNotNull(Map<String, String> fields, String key, Object value) {
        if (value != null) {
            fields.put(key, String.valueOf(value));
        }
    }
}
