package com.tianxin.lifeline.monitor.service.impl;

import com.tianxin.lifeline.monitor.dto.SensorRawMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TDengine 传感数据攒批写入器。
 *
 * <p>写入策略：消费线程将数据投递至线程安全缓冲队列，
 * 由独立调度线程按 {@code tdengine.batch-interval-ms}（默认 500ms）定时冲刷；
 * 当缓冲区达到 {@code tdengine.batch-size}（默认 2000 条）时提交线程立即触发冲刷，
 * 兼顾低延迟与高吞吐（压测模式下 100 条/消息的批量可直接进入缓冲）。
 *
 * <p>写入语句采用 TDengine 多子表批量插入语法：
 * <pre>
 * INSERT INTO t_dev_gas_001 USING sensor_data TAGS('DEV-GAS-001','GAS','pressure','AREA-001') VALUES(...)
 *          t_dev_wat_001 USING sensor_data TAGS(...) VALUES(...)
 * </pre>
 * 子表名由设备编码归一化生成（小写、非法字符替换为下划线、t_ 前缀），
 * 建表动作由 USING 子句自动完成（自动建表 + 标签写入）。
 *
 * <p>容错约定：单次冲刷异常仅记录错误日志并丢弃该批数据，
 * 不向调度器抛出异常（{@code scheduleWithFixedDelay} 遇未捕获异常会静默终止任务），
 * 也不将失败数据重新入队（避免毒数据造成无限重试风暴）。
 */
@Slf4j
@Component
public class TDengineBatchWriter {

    /** 单条 INSERT 语句最大行数（超出拆分为多条语句，防止 SQL 过长） */
    private static final int MAX_ROWS_PER_INSERT = 500;

    /** TDengine 超级表名 */
    private static final String STABLE_NAME = "sensor_data";

    /** TDengine 专用 JdbcTemplate（独立数据源，见 TDengineConfig） */
    private final JdbcTemplate tdengineJdbcTemplate;

    /** 攒批大小（条数阈值，来自 tdengine.batch-size） */
    private final int batchSize;

    /** 定时冲刷间隔（毫秒，来自 tdengine.batch-interval-ms） */
    private final long batchIntervalMs;

    /** 线程安全攒批缓冲队列 */
    private final BlockingQueue<SensorRawMessage> buffer = new LinkedBlockingQueue<>();

    /** 冲刷互斥锁（调度线程与提交线程可能同时触发冲刷） */
    private final Object flushLock = new Object();

    /** 定时冲刷调度器 */
    private ScheduledExecutorService scheduler;

    /**
     * 构造攒批写入器。
     *
     * @param tdengineJdbcTemplate TDengine 专用 JdbcTemplate
     * @param batchSize            攒批大小阈值（默认 2000）
     * @param batchIntervalMs      定时冲刷间隔毫秒（默认 500）
     */
    public TDengineBatchWriter(@Qualifier("tdengineJdbcTemplate") JdbcTemplate tdengineJdbcTemplate,
                               @Value("${tdengine.batch-size:2000}") int batchSize,
                               @Value("${tdengine.batch-interval-ms:500}") long batchIntervalMs) {
        this.tdengineJdbcTemplate = tdengineJdbcTemplate;
        this.batchSize = batchSize;
        this.batchIntervalMs = batchIntervalMs;
    }

    /**
     * 启动定时冲刷调度器（应用启动后自动执行）。
     */
    @PostConstruct
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tdengine-batch-flusher");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::flushQuietly, batchIntervalMs, batchIntervalMs, TimeUnit.MILLISECONDS);
        log.info("TDengine攒批写入器已启动: batchSize={}, batchIntervalMs={}", batchSize, batchIntervalMs);
    }

    /**
     * 停止调度器并冲刷残留缓冲（应用关闭前自动执行，尽量不丢数据）。
     */
    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        flushQuietly();
        if (!buffer.isEmpty()) {
            log.warn("TDengine攒批写入器关闭后仍有 {} 条数据未落库", buffer.size());
        }
    }

    /**
     * 提交一批传感数据至缓冲区；若缓冲达到阈值立即触发冲刷。
     *
     * @param records 传感数据列表（调用方须保证 deviceId/ts 合法）
     */
    public void submit(List<SensorRawMessage> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        buffer.addAll(records);
        if (buffer.size() >= batchSize) {
            flushQuietly();
        }
    }

    /**
     * 安全冲刷：捕获一切异常，保证调度线程不因写入失败而终止。
     */
    private void flushQuietly() {
        try {
            flush();
        } catch (Exception e) {
            log.error("TDengine定时冲刷异常", e);
        }
    }

    /**
     * 从缓冲区取出数据并执行批量写入。
     */
    private void flush() {
        List<SensorRawMessage> drained = new ArrayList<>();
        synchronized (flushLock) {
            buffer.drainTo(drained);
            if (drained.isEmpty()) {
                return;
            }
            for (int i = 0; i < drained.size(); i += MAX_ROWS_PER_INSERT) {
                List<SensorRawMessage> chunk = drained.subList(i, Math.min(i + MAX_ROWS_PER_INSERT, drained.size()));
                try {
                    tdengineJdbcTemplate.execute(buildInsertSql(chunk));
                } catch (Exception e) {
                    log.error("TDengine批量写入失败, 丢弃 {} 条数据", chunk.size(), e);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("TDengine批量写入完成: {} 条", drained.size());
        }
    }

    /**
     * 构建多子表批量 INSERT 语句。
     *
     * @param rows 本批次数据（不超过 {@link #MAX_ROWS_PER_INSERT} 条）
     * @return TDengine 多表批量插入 SQL
     */
    private String buildInsertSql(List<SensorRawMessage> rows) {
        StringBuilder sql = new StringBuilder("INSERT INTO");
        for (SensorRawMessage row : rows) {
            sql.append(' ')
                    .append(buildTableName(row.getDeviceId()))
                    .append(" USING ").append(STABLE_NAME)
                    .append(" TAGS(")
                    .append(quote(row.getDeviceId())).append(',')
                    .append(quote(row.getPipeType())).append(',')
                    .append(quote(row.getSensorType())).append(',')
                    .append(quote(row.getAreaId()))
                    .append(") VALUES(")
                    .append(row.getTs()).append(',')
                    .append(numberOrNull(row.getTemperature())).append(',')
                    .append("NULL,")  // humidity
                    .append(numberOrNull(row.getPressure())).append(',')
                    .append(numberOrNull(row.getFlow())).append(',')  // flow_rate
                    .append("NULL,")  // gas_concentration
                    .append(numberOrNull(row.getVibration())).append(',')
                    .append(row.getStatus() == null ? "NULL" : row.getStatus().toString()).append(',')  // pipe_status
                    .append(numberOrNull(row.getCorrosion())).append(',')
                    .append(numberOrNull(row.getLeakage()))
                    .append(')');
        }
        return sql.toString();
    }

    /**
     * 由设备编码生成 TDengine 子表名：加 t_ 前缀、转小写、
     * 非字母数字下划线字符统一替换为下划线（如 DEV-GAS-001 → t_dev_gas_001）。
     *
     * @param deviceId 设备编码
     * @return 合法子表名
     */
    private String buildTableName(String deviceId) {
        return "t_" + deviceId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * 将字符串包装为 SQL 字符串字面量（单引号转义），null 返回 NULL。
     *
     * @param value 标签值
     * @return SQL 字面量
     */
    private String quote(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 将数值转换为 SQL 字面量，null 返回 NULL。
     *
     * @param value 数值
     * @return SQL 字面量
     */
    private String numberOrNull(Double value) {
        return value == null ? "NULL" : value.toString();
    }
}
