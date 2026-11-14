package com.tianxin.lifeline.flink.common;

/**
 * TDengine SQL 构建工具。
 *
 * <p>子表命名与 SQL 字面量规则与 monitor-data-service 的 TDengineBatchWriter
 * 完全一致，保证 Flink 写入与既有 Spring 链路写入落在同一批子表：
 * 子表名 = "t_" + 设备编码小写 + 非法字符转下划线（DEV-GAS-001 → t_dev_gas_001）。
 */
public final class SqlUtils {

    private SqlUtils() {
    }

    /** TDengine RESTful 驱动 3.x 实际识别的 URL 前缀 */
    private static final String TAOS_RS_PREFIX = "jdbc:TAOS-RS://";

    /** 配置文件中可能使用的等价别名前缀 */
    private static final String TAOS_REST_PREFIX = "jdbc:TAOS-REST://";

    /**
     * 归一化 TDengine JDBC URL：{@code jdbc:TAOS-REST://} → {@code jdbc:TAOS-RS://}
     * （与 monitor-data-service TDengineConfig 的归一化逻辑一致）。
     *
     * @param rawUrl 原始 URL
     * @return 驱动可识别的 URL
     */
    public static String normalizeTaosUrl(String rawUrl) {
        if (rawUrl != null && rawUrl.startsWith(TAOS_REST_PREFIX)) {
            return TAOS_RS_PREFIX + rawUrl.substring(TAOS_REST_PREFIX.length());
        }
        return rawUrl;
    }

    /**
     * 由设备编码生成 TDengine 子表名：加 t_ 前缀、转小写、
     * 非字母数字下划线字符统一替换为下划线。
     *
     * @param deviceId 设备编码
     * @return 合法子表名
     */
    public static String buildTableName(String deviceId) {
        return "t_" + deviceId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    /**
     * 将字符串包装为 SQL 字符串字面量（单引号转义），null 返回 NULL。
     *
     * @param value 标签值
     * @return SQL 字面量
     */
    public static String quote(String value) {
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
    public static String num(Double value) {
        return value == null ? "NULL" : value.toString();
    }
}
