package com.tianxin.lifeline.spark.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * PostgreSQL 结果写入工具（driver 端批量 upsert）。
 *
 * <p>统计结果为小结果集（日/周聚合行数百级），driver 端
 * 批量写入即可；SQL 统一采用 {@code ON CONFLICT ... DO UPDATE}
 * 幂等 upsert，作业可重复执行不产生重复行。
 */
public final class PgSink {

    /** 批量提交批大小 */
    private static final int BATCH_SIZE = 500;

    private PgSink() {
    }

    /**
     * 批量幂等写入（单连接事务，失败整体回滚）。
     *
     * @param jdbcUrl  PostgreSQL JDBC URL
     * @param user     用户名
     * @param password 密码
     * @param upsertSql upsert SQL（含 ON CONFLICT DO UPDATE，? 占位）
     * @param rows     参数行列表（每行按占位顺序取值）
     * @throws Exception 连接/写入异常
     */
    public static void writeBatch(String jdbcUrl, String user, String password,
                                  String upsertSql, List<Object[]> rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            System.out.println("[PgSink] 无结果行, 跳过写入");
            return;
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(upsertSql)) {
                int count = 0;
                for (Object[] row : rows) {
                    for (int i = 0; i < row.length; i++) {
                        statement.setObject(i + 1, row[i]);
                    }
                    statement.addBatch();
                    if (++count % BATCH_SIZE == 0) {
                        statement.executeBatch();
                    }
                }
                statement.executeBatch();
                connection.commit();
                System.out.println("[PgSink] 写入完成, 共 " + count + " 行");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
