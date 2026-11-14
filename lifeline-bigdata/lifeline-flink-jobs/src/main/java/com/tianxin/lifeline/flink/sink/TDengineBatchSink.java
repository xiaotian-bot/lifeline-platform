package com.tianxin.lifeline.flink.sink;

import com.tianxin.lifeline.flink.common.SqlUtils;
import com.tianxin.lifeline.flink.model.SensorMessage;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

/**
 * TDengine JDBC 批量写入 Sink。
 *
 * <p>写入语句与 monitor-data-service TDengineBatchWriter 完全同构：
 * <pre>
 * INSERT INTO t_dev_gas_001 USING sensor_data TAGS('DEV-GAS-001','GAS','pressure','AREA-001') VALUES(...)
 *          t_dev_wat_001 USING sensor_data TAGS(...) VALUES(...)
 * </pre>
 * 子表由 USING 子句自动创建；单条 INSERT 语句最多 500 行（防 SQL 过长）。
 *
 * <p>连接采用 TAOS-RS RESTful 模式（参照 monitor-data-service TDengineConfig），
 * URL 前缀自动归一化（TAOS-REST → TAOS-RS）。
 *
 * <p>幂等与容错约定：TDengine 同子表同 ts 写入为覆盖语义，作业重启导致的
 * 重复投递不产生脏数据；单次写入失败仅记录错误日志并丢弃该批
 * （与 TDengineBatchWriter 的容错口径一致，避免毒数据无限重试）。
 */
public class TDengineBatchSink extends RichSinkFunction<List<SensorMessage>> {

    private static final Logger log = LoggerFactory.getLogger(TDengineBatchSink.class);

    /** 单条 INSERT 语句最大行数（超出拆分为多条语句，防止 SQL 过长） */
    private static final int MAX_ROWS_PER_INSERT = 500;

    /** TDengine 超级表名 */
    private static final String STABLE_NAME = "sensor_data";

    /** TDengine JDBC URL */
    private final String url;

    /** TDengine 用户名 */
    private final String username;

    /** TDengine 密码 */
    private final String password;

    /** JDBC 连接（open 时建立，close 时释放） */
    private transient Connection connection;

    /**
     * 构造 TDengine 写入 Sink。
     *
     * @param url      JDBC URL（jdbc:TAOS-RS://host:6041/pipe_ts）
     * @param username 用户名
     * @param password 密码
     */
    public TDengineBatchSink(String url, String username, String password) {
        this.url = SqlUtils.normalizeTaosUrl(url);
        this.username = username;
        this.password = password;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        // 显式加载 RESTful 驱动（shade 后 META-INF/services 已合并，此处双保险）
        Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
        connection = DriverManager.getConnection(url, username, password);
        log.info("TDengine JDBC 连接已建立: {}", url);
    }

    @Override
    public void invoke(List<SensorMessage> batch, Context context) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        for (int i = 0; i < batch.size(); i += MAX_ROWS_PER_INSERT) {
            List<SensorMessage> chunk = batch.subList(i, Math.min(i + MAX_ROWS_PER_INSERT, batch.size()));
            try {
                ensureConnection();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(buildInsertSql(chunk));
                }
            } catch (Exception e) {
                // 与 TDengineBatchWriter 口径一致：失败仅告警丢弃，不重试不中断
                log.error("TDengine批量写入失败, 丢弃 {} 条数据", chunk.size(), e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("TDengine批量写入完成: {} 条", batch.size());
        }
    }

    @Override
    public void close() throws Exception {
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
     * 连接保活：断开时自动重建。
     */
    private void ensureConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }

    /**
     * 构建多子表批量 INSERT 语句（列序与 02-tdengine-init.sql sensor_data 一致：
     * ts, pressure, temperature, flow, vibration, corrosion, leakage, status）。
     *
     * @param rows 本批次数据（不超过 {@link #MAX_ROWS_PER_INSERT} 条）
     * @return TDengine 多表批量插入 SQL
     */
    private String buildInsertSql(List<SensorMessage> rows) {
        StringBuilder sql = new StringBuilder("INSERT INTO");
        for (SensorMessage row : rows) {
            sql.append(' ')
                    .append(SqlUtils.buildTableName(row.getDeviceId()))
                    .append(" USING ").append(STABLE_NAME)
                    .append(" TAGS(")
                    .append(SqlUtils.quote(row.getDeviceId())).append(',')
                    .append(SqlUtils.quote(row.getPipeType())).append(',')
                    .append(SqlUtils.quote(row.getSensorType())).append(',')
                    .append(SqlUtils.quote(row.getAreaId()))
                    .append(") VALUES(")
                    .append(row.getTs()).append(',')
                    .append(SqlUtils.num(row.getPressure())).append(',')
                    .append(SqlUtils.num(row.getTemperature())).append(',')
                    .append(SqlUtils.num(row.getFlow())).append(',')
                    .append(SqlUtils.num(row.getVibration())).append(',')
                    .append(SqlUtils.num(row.getCorrosion())).append(',')
                    .append(SqlUtils.num(row.getLeakage())).append(',')
                    .append(row.getStatus() == null ? "NULL" : row.getStatus().toString())
                    .append(')');
        }
        return sql.toString();
    }
}
