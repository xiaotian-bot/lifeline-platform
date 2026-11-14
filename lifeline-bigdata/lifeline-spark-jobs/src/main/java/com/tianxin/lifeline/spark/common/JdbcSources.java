package com.tianxin.lifeline.spark.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC 数据拉取工具（driver 端一次性拉取，轻量离线场景）。
 *
 * <p>设计决策：P10-1 离线作业数据量为「日/周」级别，driver 端 JDBC 拉取后
 * parallelize 为 RDD/DataFrame 再交由 Spark 聚合，避免引入 spark-external
 * 重型连接器依赖；后续数据量膨胀可平滑切换 Spark JDBC DataSource 分区读。
 */
public final class JdbcSources {

    /** JDBC 拉取批次日志阈值（行数） */
    private static final int LOG_BATCH_SIZE = 100_000;

    private JdbcSources() {
    }

    /**
     * 行映射函数：ResultSet 当前行 → 目标对象。
     *
     * @param <T> 目标类型
     */
    @FunctionalInterface
    public interface RowMapper<T> {

        /**
         * 映射当前行。
         *
         * @param rs 结果集（游标已定位当前行）
         * @return 映射结果，返回 null 表示跳过该行
         * @throws Exception SQL 访问异常
         */
        T map(ResultSet rs) throws Exception;
    }

    /**
     * 执行查询并映射全部行。
     *
     * @param jdbcUrl  JDBC URL（TDengine / PostgreSQL 均可）
     * @param user     用户名
     * @param password 密码
     * @param sql      查询 SQL（? 占位参数由 bindValues 顺序绑定）
     * @param mapper   行映射函数
     * @param bindValues 绑定参数值（可为空数组）
     * @param <T>      目标类型
     * @return 映射结果列表（不含 null）
     * @throws Exception 连接/查询异常
     */
    public static <T> List<T> query(String jdbcUrl, String user, String password,
                                    String sql, RowMapper<T> mapper, Object... bindValues)
            throws Exception {
        List<T> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, bindValues);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    T mapped = mapper.map(resultSet);
                    if (mapped != null) {
                        results.add(mapped);
                    }
                    if (results.size() % LOG_BATCH_SIZE == 0) {
                        System.out.println("[JdbcSources] 已拉取 " + results.size() + " 行...");
                    }
                }
            }
        }
        return results;
    }

    /**
     * 绑定 PreparedStatement 参数（null 以 setNull Object 处理）。
     *
     * @param statement  语句
     * @param bindValues 参数值
     * @throws Exception SQL 异常
     */
    private static void bind(PreparedStatement statement, Object... bindValues) throws Exception {
        if (bindValues == null) {
            return;
        }
        for (int i = 0; i < bindValues.length; i++) {
            statement.setObject(i + 1, bindValues[i]);
        }
    }
}
