package com.tianxin.lifeline.spark.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Spark 离线作业启动参数（连接信息与日期范围全部参数化）。
 *
 * <p>参数解析优先级：main 方法 --key value 命令行参数 &gt; 环境变量 &gt; 默认值。
 * 环境变量名为参数名大写且 '-' 转 '_'（如 --pg-url 对应 PG_URL）。
 * 支持的公共参数（均可选）：
 * <pre>
 * --master            Spark master（默认 local[*]，集群提交时 spark-submit 指定）
 * --pg-url            PostgreSQL JDBC URL（默认 jdbc:postgresql://localhost:5432/lifeline）
 * --pg-user           PostgreSQL 用户名（默认 lifeline）
 * --pg-password       PostgreSQL 密码（默认 lifeline123）
 * --tdengine-url      TDengine JDBC URL（默认 jdbc:TAOS://localhost:6030/pipe_ts）
 * --tdengine-user     TDengine 用户名（默认 root）
 * --tdengine-password TDengine 密码（默认 taosdata）
 * --date              统计目标日 yyyy-MM-dd（默认昨天）
 * --start-date        范围起始日 yyyy-MM-dd（趋势/训练集作业）
 * --end-date          范围结束日 yyyy-MM-dd（默认 --date）
 * --output-dir        输出目录（训练集作业）
 * --format            输出格式 csv/parquet（训练集作业，默认 parquet）
 * --sample-ratio      抽样比例 0-1（训练集作业，默认 1.0）
 * --gap-threshold-seconds 时间断档阈值秒（质量稽核作业，默认 60）
 * </pre>
 */
public final class SparkJobParams {

    /** 日期参数格式 */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 默认 PostgreSQL JDBC URL（对齐 docker-compose postgres 服务） */
    public static final String DEFAULT_PG_URL = "jdbc:postgresql://localhost:5432/lifeline";

    /** 默认 TDengine JDBC URL（对齐 docker-compose tdengine 服务原生连接端口 6030） */
    public static final String DEFAULT_TDENGINE_URL = "jdbc:TAOS://localhost:6030/pipe_ts";

    /** 解析后的参数表 */
    private final Map<String, String> params = new HashMap<>();

    private SparkJobParams() {
    }

    /**
     * 解析 main 方法参数（--key value 成对形式）。
     *
     * @param args 命令行参数
     * @return 参数对象
     */
    public static SparkJobParams parse(String[] args) {
        SparkJobParams jobParams = new SparkJobParams();
        if (args == null) {
            return jobParams;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i] != null && args[i].startsWith("--")) {
                jobParams.params.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return jobParams;
    }

    /**
     * 取参数值：命令行参数 &gt; 环境变量（key 大写、'-' 转 '_'）&gt; 默认值。
     *
     * @param key          参数名（如 pg-url）
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String get(String key, String defaultValue) {
        String value = params.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        String envKey = key.toUpperCase().replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    /**
     * 取浮点参数。
     *
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值，格式非法时返回默认值
     */
    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 取长整型参数。
     *
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值，格式非法时返回默认值
     */
    public long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Spark master（默认本地模式 local[*]，集群提交经 spark-submit --master 覆盖） */
    public String master() {
        return get("master", "local[*]");
    }

    /** PostgreSQL JDBC URL */
    public String pgUrl() {
        return get("pg-url", DEFAULT_PG_URL);
    }

    /** PostgreSQL 用户名 */
    public String pgUser() {
        return get("pg-user", "lifeline");
    }

    /** PostgreSQL 密码 */
    public String pgPassword() {
        return get("pg-password", "lifeline123");
    }

    /** TDengine JDBC URL */
    public String tdengineUrl() {
        return get("tdengine-url", DEFAULT_TDENGINE_URL);
    }

    /** TDengine 用户名 */
    public String tdengineUser() {
        return get("tdengine-user", "root");
    }

    /** TDengine 密码 */
    public String tdenginePassword() {
        return get("tdengine-password", "taosdata");
    }

    /**
     * 统计目标日（默认昨天）。
     *
     * @return 目标日期
     */
    public LocalDate date() {
        return parseDate(get("date", LocalDate.now().minusDays(1).format(DATE_FORMAT)),
                LocalDate.now().minusDays(1));
    }

    /**
     * 范围起始日（默认等于目标日）。
     *
     * @return 起始日期
     */
    public LocalDate startDate() {
        return parseDate(get("start-date", date().format(DATE_FORMAT)), date());
    }

    /**
     * 范围结束日（默认等于目标日）。
     *
     * @return 结束日期
     */
    public LocalDate endDate() {
        return parseDate(get("end-date", date().format(DATE_FORMAT)), date());
    }

    /**
     * 解析日期参数，非法格式回退默认值。
     *
     * @param text         日期文本（yyyy-MM-dd）
     * @param defaultValue 默认值
     * @return 解析结果
     */
    private LocalDate parseDate(String text, LocalDate defaultValue) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
