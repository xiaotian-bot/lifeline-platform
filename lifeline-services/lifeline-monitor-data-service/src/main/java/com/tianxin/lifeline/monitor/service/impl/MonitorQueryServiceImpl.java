package com.tianxin.lifeline.monitor.service.impl;

import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.monitor.dto.SensorDataVO;
import com.tianxin.lifeline.monitor.dto.SensorStatisticsVO;
import com.tianxin.lifeline.monitor.service.MonitorQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 监测数据查询服务实现。
 *
 * <p>查询统一走 TDengine 专用 JdbcTemplate（tdengineJdbcTemplate）与 Redis；
 * deviceId 在入参校验阶段以白名单正则限定为字母/数字/下划线/中划线，
 * 其后直接拼接 SQL 不存在注入风险（TDengine REST 驱动的 PreparedStatement
 * 对 INTERVAL 等语法支持有限，故采用校验后拼接方案）。
 *
 * <p><b>deviceId 语义</b>：一律为设备编码字符串（如 DEV-GAS-001），
 * 与 TDengine 标签 device_id、Redis key {@code rt:{deviceId}} 对齐。
 */
@Slf4j
@Service
public class MonitorQueryServiceImpl implements MonitorQueryService {

    /** Redis 实时缓存 key 前缀（与写入端保持一致） */
    private static final String REDIS_KEY_PREFIX = "rt:";

    /** 查询返回条数硬上限 */
    private static final int MAX_LIMIT = 10000;

    /** 统计查询最大时间跨度（366 天，毫秒） */
    private static final long MAX_STATISTICS_SPAN_MS = 366L * 24 * 60 * 60 * 1000;

    /** 设备编码白名单：字母/数字/下划线/中划线，1~64 位（防 SQL 注入） */
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,64}");

    /** 历史聚合间隔格式：数字 + s/m/h/d，如 5m、1h、1d */
    private static final Pattern INTERVAL_PATTERN = Pattern.compile("[1-9]\\d{0,3}[smhd]");

    /** 统计粒度格式：仅小时/天，如 1h、6h、1d */
    private static final Pattern STATISTICS_INTERVAL_PATTERN = Pattern.compile("[1-9]\\d{0,2}[hd]");

    /** TDengine 专用 JdbcTemplate */
    private final JdbcTemplate tdengineJdbcTemplate;

    /** Redis 操作模板 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造查询服务。
     *
     * @param tdengineJdbcTemplate TDengine 专用 JdbcTemplate
     * @param stringRedisTemplate  Redis 操作模板
     */
    public MonitorQueryServiceImpl(@Qualifier("tdengineJdbcTemplate") JdbcTemplate tdengineJdbcTemplate,
                                   StringRedisTemplate stringRedisTemplate) {
        this.tdengineJdbcTemplate = tdengineJdbcTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorDataVO getRealtime(String deviceId) {
        validateDeviceId(deviceId);
        SensorDataVO cached = readFromRedis(deviceId);
        if (cached != null) {
            return cached;
        }
        // Redis miss：回源 TDengine 最新一条
        String sql = "SELECT ts, temperature, humidity, pressure, flow_rate, gas_concentration, vibration, pipe_status, corrosion, leakage, "
                + "pipe_type, sensor_type, area_id FROM sensor_data "
                + "WHERE device_id = '" + deviceId + "' ORDER BY ts DESC LIMIT 1";
        List<SensorDataVO> rows = tdengineJdbcTemplate.query(sql, (rs, rowNum) -> mapDetailRow(rs, deviceId));
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "设备监测数据不存在: " + deviceId);
        }
        return rows.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SensorDataVO> queryHistory(String deviceId, Long startTime, Long endTime,
                                           String interval, Integer limit) {
        validateDeviceId(deviceId);
        validateTimeRange(startTime, endTime);
        int safeLimit = normalizeLimit(limit);

        if (interval != null && !interval.isBlank()) {
            String safeInterval = validateInterval(interval.trim(), INTERVAL_PATTERN,
                    "历史聚合间隔格式非法, 应为 数字+时间单位(s/m/h/d), 如 5m/1h");
            // INTERVAL 聚合：各指标取窗口均值, ts 为窗口起始时间
            String sql = "SELECT _wstart AS bucket_ts, AVG(pressure) AS pressure, "
                    + "AVG(temperature) AS temperature, AVG(flow_rate) AS flow_rate, "
                    + "AVG(vibration) AS vibration, AVG(corrosion) AS corrosion, "
                    + "AVG(leakage) AS leakage, LAST(pipe_status) AS pipe_status "
                    + "FROM sensor_data WHERE device_id = '" + deviceId + "' "
                    + "AND ts >= " + startTime + " AND ts <= " + endTime + " "
                    + "INTERVAL(" + safeInterval + ") LIMIT " + safeLimit;
            return tdengineJdbcTemplate.query(sql, (rs, rowNum) -> mapAggregatedRow(rs, deviceId));
        }

        // 原始明细查询
        String sql = "SELECT ts, temperature, humidity, pressure, flow_rate, gas_concentration, vibration, pipe_status, corrosion, leakage, "
                + "pipe_type, sensor_type, area_id FROM sensor_data "
                + "WHERE device_id = '" + deviceId + "' "
                + "AND ts >= " + startTime + " AND ts <= " + endTime + " "
                + "ORDER BY ts DESC LIMIT " + safeLimit;
        return tdengineJdbcTemplate.query(sql, (rs, rowNum) -> mapDetailRow(rs, deviceId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SensorStatisticsVO> queryStatistics(String deviceId, Long startTime, Long endTime,
                                                    String interval) {
        validateDeviceId(deviceId);
        validateTimeRange(startTime, endTime);
        if (interval == null || interval.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "统计粒度 interval 不能为空");
        }
        if (endTime - startTime > MAX_STATISTICS_SPAN_MS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "统计时间跨度不得超过366天");
        }
        String safeInterval = validateInterval(interval.trim(), STATISTICS_INTERVAL_PATTERN,
                "统计粒度仅支持小时(h)/天(d), 如 1h/6h/1d");

        String sql = "SELECT _wstart AS bucket_ts, COUNT(*) AS cnt, "
                + "MAX(pressure) AS max_pressure, MIN(pressure) AS min_pressure, AVG(pressure) AS avg_pressure, "
                + "MAX(temperature) AS max_temperature, MIN(temperature) AS min_temperature, "
                + "AVG(temperature) AS avg_temperature, "
                + "MAX(flow_rate) AS max_flow, MIN(flow_rate) AS min_flow, AVG(flow_rate) AS avg_flow, "
                + "MAX(vibration) AS max_vibration, MIN(vibration) AS min_vibration, AVG(vibration) AS avg_vibration, "
                + "MAX(corrosion) AS max_corrosion, MIN(corrosion) AS min_corrosion, AVG(corrosion) AS avg_corrosion, "
                + "MAX(leakage) AS max_leakage, MIN(leakage) AS min_leakage, AVG(leakage) AS avg_leakage "
                + "FROM sensor_data WHERE device_id = '" + deviceId + "' "
                + "AND ts >= " + startTime + " AND ts < " + endTime + " "
                + "INTERVAL(" + safeInterval + ") LIMIT " + MAX_LIMIT;
        return tdengineJdbcTemplate.query(sql, (rs, rowNum) -> mapStatisticsRow(rs, deviceId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SensorDataVO> querySensorData(String deviceId, Integer limit) {
        validateDeviceId(deviceId);
        int safeLimit = normalizeLimit(limit != null ? limit : 20);
        String sql = "SELECT ts, temperature, humidity, pressure, flow_rate, gas_concentration, vibration, pipe_status, corrosion, leakage, "
                + "pipe_type, sensor_type, area_id FROM sensor_data "
                + "WHERE device_id = '" + deviceId + "' ORDER BY ts DESC LIMIT " + safeLimit;
        return tdengineJdbcTemplate.query(sql, (rs, rowNum) -> mapDetailRow(rs, deviceId));
    }

    /* ==================== 参数校验 ==================== */

    /**
     * 校验设备编码格式（白名单正则，兼顾合法性与防注入）。
     *
     * @param deviceId 设备编码
     */
    private void validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "设备编码不能为空");
        }
        if (!DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "设备编码格式非法: " + deviceId);
        }
    }

    /**
     * 强制校验时间范围：起止均非空且 start &lt; end。
     *
     * @param startTime 开始时间（毫秒）
     * @param endTime   结束时间（毫秒）
     */
    private void validateTimeRange(Long startTime, Long endTime) {
        if (startTime == null || endTime == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "startTime/endTime 不能为空");
        }
        if (startTime >= endTime) {
            throw new BizException(ErrorCode.PARAM_INVALID, "startTime 必须早于 endTime");
        }
    }

    /**
     * 归一化 limit：空/非正取默认 100，超过硬上限 10000 截断。
     *
     * @param limit 请求 limit
     * @return 合法 limit
     */
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 校验聚合间隔格式（白名单正则，防注入）。
     *
     * @param interval 聚合间隔
     * @param pattern  格式正则
     * @param message  错误提示
     * @return 校验通过的间隔字符串
     */
    private String validateInterval(String interval, Pattern pattern, String message) {
        if (!pattern.matcher(interval).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
        return interval;
    }

    /* ==================== Redis 读取 ==================== */

    /**
     * 从 Redis HASH 读取设备实时缓存。
     *
     * @param deviceId 设备编码
     * @return 实时数据，缓存不存在返回 null
     */
    private SensorDataVO readFromRedis(String deviceId) {
        Map<Object, Object> entries;
        try {
            entries = stringRedisTemplate.opsForHash().entries(REDIS_KEY_PREFIX + deviceId);
        } catch (Exception e) {
            log.warn("Redis实时缓存读取失败, 降级回源TDengine: deviceId={}", deviceId, e);
            return null;
        }
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        SensorDataVO vo = new SensorDataVO();
        vo.setDeviceId(deviceId);
        vo.setPipeType(asString(entries.get("pipe_type")));
        vo.setSensorType(asString(entries.get("sensor_type")));
        vo.setAreaId(asString(entries.get("area_id")));
        vo.setTs(asLong(entries.get("ts")));
        vo.setPressure(asDouble(entries.get("pressure")));
        vo.setTemperature(asDouble(entries.get("temperature")));
        vo.setFlow(asDouble(entries.get("flow_rate")));
        vo.setVibration(asDouble(entries.get("vibration")));
        vo.setCorrosion(asDouble(entries.get("corrosion")));
        vo.setLeakage(asDouble(entries.get("leakage")));
        vo.setStatus(asInteger(entries.get("pipe_status")));
        return vo;
    }

    /* ==================== 结果集映射 ==================== */

    /**
     * 映射明细行（原始数据或回源查询）。
     *
     * @param rs       结果集
     * @param deviceId 设备编码
     * @return 传感数据 VO
     * @throws SQLException SQL 异常
     */
    private SensorDataVO mapDetailRow(ResultSet rs, String deviceId) throws SQLException {
        SensorDataVO vo = new SensorDataVO();
        vo.setDeviceId(deviceId);
        vo.setTs(timestampMillis(rs, "ts"));
        vo.setPressure(nullableDouble(rs, "pressure"));
        vo.setTemperature(nullableDouble(rs, "temperature"));
        vo.setFlow(nullableDouble(rs, "flow_rate"));
        vo.setVibration(nullableDouble(rs, "vibration"));
        vo.setCorrosion(nullableDouble(rs, "corrosion"));
        vo.setLeakage(nullableDouble(rs, "leakage"));
        vo.setStatus(nullableInteger(rs, "pipe_status"));
        vo.setPipeType(nullableString(rs, "pipe_type"));
        vo.setSensorType(nullableString(rs, "sensor_type"));
        vo.setAreaId(nullableString(rs, "area_id"));
        return vo;
    }

    /**
     * 映射 INTERVAL 聚合行（历史聚合查询，指标为窗口均值）。
     *
     * @param rs       结果集
     * @param deviceId 设备编码
     * @return 传感数据 VO（ts 为窗口起始时间）
     * @throws SQLException SQL 异常
     */
    private SensorDataVO mapAggregatedRow(ResultSet rs, String deviceId) throws SQLException {
        SensorDataVO vo = new SensorDataVO();
        vo.setDeviceId(deviceId);
        vo.setTs(timestampMillis(rs, "bucket_ts"));
        vo.setPressure(nullableDouble(rs, "pressure"));
        vo.setTemperature(nullableDouble(rs, "temperature"));
        vo.setFlow(nullableDouble(rs, "flow_rate"));
        vo.setVibration(nullableDouble(rs, "vibration"));
        vo.setCorrosion(nullableDouble(rs, "corrosion"));
        vo.setLeakage(nullableDouble(rs, "leakage"));
        vo.setStatus(nullableInteger(rs, "pipe_status"));
        return vo;
    }

    /**
     * 映射统计聚合行（max/min/avg 指标）。
     *
     * @param rs       结果集
     * @param deviceId 设备编码
     * @return 统计 VO
     * @throws SQLException SQL 异常
     */
    private SensorStatisticsVO mapStatisticsRow(ResultSet rs, String deviceId) throws SQLException {
        SensorStatisticsVO vo = new SensorStatisticsVO();
        vo.setDeviceId(deviceId);
        vo.setTs(timestampMillis(rs, "bucket_ts"));
        vo.setCount(rs.getLong("cnt"));
        vo.setMaxPressure(nullableDouble(rs, "max_pressure"));
        vo.setMinPressure(nullableDouble(rs, "min_pressure"));
        vo.setAvgPressure(nullableDouble(rs, "avg_pressure"));
        vo.setMaxTemperature(nullableDouble(rs, "max_temperature"));
        vo.setMinTemperature(nullableDouble(rs, "min_temperature"));
        vo.setAvgTemperature(nullableDouble(rs, "avg_temperature"));
        vo.setMaxFlow(nullableDouble(rs, "max_flow"));
        vo.setMinFlow(nullableDouble(rs, "min_flow"));
        vo.setAvgFlow(nullableDouble(rs, "avg_flow"));
        vo.setMaxVibration(nullableDouble(rs, "max_vibration"));
        vo.setMinVibration(nullableDouble(rs, "min_vibration"));
        vo.setAvgVibration(nullableDouble(rs, "avg_vibration"));
        vo.setMaxCorrosion(nullableDouble(rs, "max_corrosion"));
        vo.setMinCorrosion(nullableDouble(rs, "min_corrosion"));
        vo.setAvgCorrosion(nullableDouble(rs, "avg_corrosion"));
        vo.setMaxLeakage(nullableDouble(rs, "max_leakage"));
        vo.setMinLeakage(nullableDouble(rs, "min_leakage"));
        vo.setAvgLeakage(nullableDouble(rs, "avg_leakage"));
        return vo;
    }

    /* ==================== 通用转换工具 ==================== */

    /**
     * 读取时间戳列并转为毫秒值。
     *
     * @param rs  结果集
     * @param col 列名
     * @return 毫秒时间戳，null 安全
     * @throws SQLException SQL 异常
     */
    private Long timestampMillis(ResultSet rs, String col) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(col);
        return timestamp == null ? null : timestamp.getTime();
    }

    /**
     * 读取可空 DOUBLE 列。
     *
     * @param rs  结果集
     * @param col 列名
     * @return 数值，SQL NULL 返回 null
     * @throws SQLException SQL 异常
     */
    private Double nullableDouble(ResultSet rs, String col) throws SQLException {
        double value = rs.getDouble(col);
        return rs.wasNull() ? null : value;
    }

    /**
     * 读取可空 INT 列。
     *
     * @param rs  结果集
     * @param col 列名
     * @return 数值，SQL NULL 返回 null
     * @throws SQLException SQL 异常
     */
    private Integer nullableInteger(ResultSet rs, String col) throws SQLException {
        int value = rs.getInt(col);
        return rs.wasNull() ? null : value;
    }

    /**
     * 读取可空字符串列。
     *
     * @param rs  结果集
     * @param col 列名
     * @return 字符串，SQL NULL 返回 null
     * @throws SQLException SQL 异常
     */
    private String nullableString(ResultSet rs, String col) throws SQLException {
        return rs.getString(col);
    }

    /**
     * Redis HASH 值转字符串。
     *
     * @param value HASH 值
     * @return 字符串，null 安全
     */
    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Redis HASH 值转 Long。
     *
     * @param value HASH 值
     * @return Long，null 或格式非法返回 null
     */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Redis HASH 值转 Double。
     *
     * @param value HASH 值
     * @return Double，null 或格式非法返回 null
     */
    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Redis HASH 值转 Integer。
     *
     * @param value HASH 值
     * @return Integer，null 或格式非法返回 null
     */
    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
