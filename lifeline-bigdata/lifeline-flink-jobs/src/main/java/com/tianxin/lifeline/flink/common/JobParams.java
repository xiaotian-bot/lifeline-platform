package com.tianxin.lifeline.flink.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Flink 作业启动参数（并行度等全部参数化）。
 *
 * <p>参数解析优先级：main 方法 --key value 命令行参数 &gt; 环境变量 &gt; 默认值。
 * 支持的参数（均可选）：
 * <pre>
 * --parallelism          作业并行度（默认 4，压测可配 16）
 * --kafka                Kafka bootstrap servers（默认 kafka:9094）
 * --checkpoint-dir       Checkpoint 目录（默认 file:///opt/flink/checkpoints）
 * --tdengine-url         TDengine JDBC URL（默认 jdbc:TAOS-RS://tdengine:6041/pipe_ts）
 * --tdengine-user        TDengine 用户名（默认 root）
 * --tdengine-password    TDengine 密码（默认 taosdata）
 * --pg-url               PostgreSQL JDBC URL（RuleWarningJob 规则源）
 * --pg-user              PostgreSQL 用户名
 * --pg-password          PostgreSQL 密码
 * --rule-refresh-ms      规则刷新间隔毫秒（默认 60000）
 * --suppress-minutes     预警抑制窗口分钟（默认 10）
 * </pre>
 */
public final class JobParams {

    /** 默认并行度 */
    public static final int DEFAULT_PARALLELISM = 4;

    /** 最大建议并行度（压测场景可配至 16） */
    public static final int MAX_SUGGESTED_PARALLELISM = 16;

    /** 解析后的参数表 */
    private final Map<String, String> params = new HashMap<>();

    private JobParams() {
    }

    /**
     * 解析 main 方法参数（--key value 成对形式）。
     *
     * @param args 命令行参数
     * @return 参数对象
     */
    public static JobParams parse(String[] args) {
        JobParams jobParams = new JobParams();
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
     * @param key          参数名（如 kafka）
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
     * 取整型参数。
     *
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值，格式非法时返回默认值
     */
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
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

    /** Kafka bootstrap servers（容器内默认读环境变量 KAFKA_BOOTSTRAP_SERVERS） */
    public String kafkaBootstrap() {
        return get("kafka", "kafka:9094");
    }

    /** 作业并行度（默认 4，可通过 --parallelism 16 提升） */
    public int parallelism() {
        return getInt("parallelism", DEFAULT_PARALLELISM);
    }

    /** Checkpoint 目录 */
    public String checkpointDir() {
        return get("checkpoint-dir", "file:///opt/flink/checkpoints");
    }
}
