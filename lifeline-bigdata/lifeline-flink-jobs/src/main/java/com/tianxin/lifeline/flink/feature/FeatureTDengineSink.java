package com.tianxin.lifeline.flink.feature;

import com.tianxin.lifeline.flink.common.SqlUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * sensor_feature 特征表写入 Sink（TDengine JDBC，TAOS-RS RESTful）。
 *
 * <p>写入语句：
 * <pre>
 * INSERT INTO f_dev_gas_001_1m USING sensor_feature
 *   TAGS('DEV-GAS-001','GAS','pressure','1m')
 *   VALUES(ts,'pressure',avg,peak,std,rate,count,start,end)
 * </pre>
 * 子表名 = f_ + 设备编码归一化 + _ + 窗口粒度（1m/5m/1h），由 USING 自动建表。
 *
 * <p>特征行产出速率远低于原始数据（每设备每指标每滑窗一行），
 * 缓冲 50 行或作业关闭时冲刷；同 (子表, ts, metric_name) 重复写入为覆盖语义，
 * 作业重启重复投递不产生脏数据。
 */
public class FeatureTDengineSink extends RichSinkFunction<FeatureRow> {

    private static final Logger log = LoggerFactory.getLogger(FeatureTDengineSink.class);

    /** 特征表超级表名 */
    private static final String STABLE_NAME = "sensor_feature";

    /** 单次冲刷缓冲阈值（行） */
    private static final int FLUSH_ROWS = 50;

    /** TDengine JDBC URL */
    private final String url;

    /** TDengine 用户名 */
    private final String username;

    /** TDengine 密码 */
    private final String password;

    /** JDBC 连接 */
    private transient Connection connection;

    /** 写入缓冲 */
    private transient List<FeatureRow> buffer;

    /**
     * 构造特征表写入 Sink。
     *
     * @param url      JDBC URL
     * @param username 用户名
     * @param password 密码
     */
    public FeatureTDengineSink(String url, String username, String password) {
        this.url = SqlUtils.normalizeTaosUrl(url);
        this.username = username;
        this.password = password;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
        connection = DriverManager.getConnection(url, username, password);
        buffer = new ArrayList<>(FLUSH_ROWS);
        log.info("sensor_feature 写入连接已建立: {}", url);
    }

    @Override
    public void invoke(FeatureRow row, Context context) {
        if (row == null || row.getDeviceId() == null || row.getTs() == null) {
            return;
        }
        buffer.add(row);
        if (buffer.size() >= FLUSH_ROWS) {
            flush();
        }
    }

    @Override
    public void close() throws Exception {
        flush();
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.warn("TDengine连接关闭异常", e);
            }
        }
        super.close();
    }

    /**
     * 冲刷缓冲：拼多子表 INSERT 一次执行；失败仅告警丢弃（特征可由窗口重算恢复）。
     */
    private void flush() {
        if (buffer == null || buffer.isEmpty()) {
            return;
        }
        List<FeatureRow> drained = new ArrayList<>(buffer);
        buffer.clear();
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, username, password);
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(buildInsertSql(drained));
            }
            if (log.isDebugEnabled()) {
                log.debug("sensor_feature写入完成: {} 行", drained.size());
            }
        } catch (Exception e) {
            log.error("sensor_feature写入失败, 丢弃 {} 行", drained.size(), e);
        }
    }

    /**
     * 构建特征表多子表批量 INSERT。
     *
     * @param rows 特征行
     * @return TDengine 多表批量插入 SQL
     */
    private String buildInsertSql(List<FeatureRow> rows) {
        StringBuilder sql = new StringBuilder("INSERT INTO");
        for (FeatureRow row : rows) {
            sql.append(' ')
                    .append(buildFeatureTableName(row.getDeviceId(), row.getIntervalType()))
                    .append(" USING ").append(STABLE_NAME)
                    .append(" TAGS(")
                    .append(SqlUtils.quote(row.getDeviceId())).append(',')
                    .append(SqlUtils.quote(row.getPipeType())).append(',')
                    .append(SqlUtils.quote(row.getSensorType())).append(',')
                    .append(SqlUtils.quote(row.getIntervalType()))
                    .append(") VALUES(")
                    .append(row.getTs()).append(',')
                    .append(SqlUtils.quote(row.getMetricName())).append(',')
                    .append(SqlUtils.num(row.getAvgValue())).append(',')
                    .append(SqlUtils.num(row.getPeakValue())).append(',')
                    .append(SqlUtils.num(row.getStdValue())).append(',')
                    .append(SqlUtils.num(row.getRateOfChange())).append(',')
                    .append(row.getSampleCount() == null ? "NULL" : row.getSampleCount().toString())
                    .append(',')
                    .append(row.getWindowStart()).append(',')
                    .append(row.getWindowEnd())
                    .append(')');
        }
        return sql.toString();
    }

    /**
     * 特征子表名：f_ + 设备编码归一化 + _ + 窗口粒度
     * （如 DEV-GAS-001 / 1m → f_dev_gas_001_1m）。
     *
     * @param deviceId     设备编码
     * @param intervalType 窗口粒度
     * @return 合法子表名
     */
    private String buildFeatureTableName(String deviceId, String intervalType) {
        String interval = intervalType == null ? "raw" : intervalType.toLowerCase().replaceAll("[^a-z0-9]", "");
        return "f_" + deviceId.toLowerCase().replaceAll("[^a-z0-9_]", "_") + "_" + interval;
    }
}
