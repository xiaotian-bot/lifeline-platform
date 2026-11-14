package com.tianxin.lifeline.spark.quality;

import com.tianxin.lifeline.spark.common.JdbcSources;
import com.tianxin.lifeline.spark.common.PgSink;
import com.tianxin.lifeline.spark.common.SparkJobParams;
import com.tianxin.lifeline.spark.common.SparkSessionFactory;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;

/**
 * 作业四：数据质量稽核（DataQualityJob）。
 *
 * <p>对目标日（--date，默认昨天）的 TDengine {@code sensor_data} 原始监测数据
 * 做四维质量稽核，结果按 设备×日×维度 粒度写入 PG {@code data_quality_audit}：
 * <ol>
 *     <li><b>完整性 COMPLETENESS</b>：以每日理论采样点数（24h × 60min = 1440，
 *         可通过 {@code --expected-per-day} 调整）为期望值，与实际点数比对；
 *         实际点数 / 期望点数 ≥ 90% 视为通过。</li>
 *     <li><b>有效性 VALIDITY</b>：六类指标（pressure / temperature / flow /
 *         vibration / corrosion / leakage）逐条检查 NULL 与越界（各指标
 *         物理合理区间硬编码于 {@link #VALID_RANGES}），越界条目标入失败计数；
 *         全部合法视为通过。</li>
 *     <li><b>一致性 CONSISTENCY</b>：同一设备同日内是否存在重复时间戳
 *         （count vs countDistinct ts），重复数 = 0 视为通过。</li>
 *     <li><b>时效性 TIMELINESS</b>：相邻采样点时间间隔超过 5 分钟
 *         （可通过 {@code --gap-threshold-min} 调整）的间隙数，
 *         间隙数 = 0 视为通过。</li>
 * </ol>
 *
 * <p>实现方式：driver 端 JDBC 拉取日级数据 → parallelize 为 DataFrame →
 * Spark 按 device_id 分组聚合 → driver 端 ON CONFLICT 幂等 upsert。
 *
 * <p>提交示例：
 * {@code spark-submit --class com.tianxin.lifeline.spark.quality.DataQualityJob
 * lifeline-spark-jobs.jar --date 2026-08-17
 * --tdengine-url jdbc:TAOS://localhost:6030/pipe_ts
 * --pg-url jdbc:postgresql://localhost:5432/lifeline}
 */
public final class DataQualityJob {

    /** 六类监测指标物理合理区间（闭区间），越界判定 */
    private static final Map<String, double[]> VALID_RANGES = new HashMap<>();

    static {
        VALID_RANGES.put("pressure",    new double[]{-1.0, 50.0});
        VALID_RANGES.put("temperature", new double[]{-50.0, 300.0});
        VALID_RANGES.put("flow",        new double[]{-100.0, 10000.0});
        VALID_RANGES.put("vibration",   new double[]{-50.0, 50.0});
        VALID_RANGES.put("corrosion",   new double[]{-10.0, 100.0});
        VALID_RANGES.put("leakage",     new double[]{-10.0, 100.0});
    }

    /** 指标名数组（与 sensor_data 列对齐） */
    private static final String[] METRICS =
            {"pressure", "temperature", "flow", "vibration", "corrosion", "leakage"};

    /** 完整性维度名 */
    private static final String DIM_COMPLETENESS = "COMPLETENESS";
    /** 有效性维度名 */
    private static final String DIM_VALIDITY = "VALIDITY";
    /** 一致性维度名 */
    private static final String DIM_CONSISTENCY = "CONSISTENCY";
    /** 时效性维度名 */
    private static final String DIM_TIMELINESS = "TIMELINESS";

    /** 完整性通过率阈值：实际/期望 ≥ 90% 视为通过 */
    private static final double COMPLETENESS_PASS_RATE = 0.9;

    /** data_quality_audit 幂等 upsert SQL */
    private static final String UPSERT_SQL = """
            INSERT INTO data_quality_audit (id, audit_date, device_id, pipe_type, dimension,
                                            expected_count, actual_count, pass_count, fail_count,
                                            pass_rate, fail_detail)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (audit_date, device_id, dimension) WHERE deleted = 0 DO UPDATE SET
                actual_count  = EXCLUDED.actual_count,
                pass_count    = EXCLUDED.pass_count,
                fail_count    = EXCLUDED.fail_count,
                pass_rate     = EXCLUDED.pass_rate,
                fail_detail   = EXCLUDED.fail_detail,
                updated_at    = CURRENT_TIMESTAMP
            """;

    private DataQualityJob() {
    }

    /**
     * 作业入口。
     *
     * @param args 启动参数（见 {@link SparkJobParams}）
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        SparkJobParams params = SparkJobParams.parse(args);
        LocalDate auditDate = params.date();
        int expectedPerDay = (int) params.getLong("expected-per-day", 1440L);
        int gapThresholdMin = (int) params.getLong("gap-threshold-min", 5L);

        try (SparkSession spark = SparkSessionFactory.create("lifeline-data-quality-job", params)) {
            JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

            // 拉取目标日全部原始数据（device_id / pipe_type / ts / 六指标）
            List<Row> rawRows = fetchRawData(params, auditDate);
            if (rawRows.isEmpty()) {
                System.out.println("[DataQualityJob] 目标日无数据: date=" + auditDate);
                return;
            }

            List<Object[]> resultRows = new ArrayList<>();
            resultRows.addAll(auditCompleteness(spark, sc, rawRows, auditDate, expectedPerDay));
            resultRows.addAll(auditValidity(spark, sc, rawRows, auditDate));
            resultRows.addAll(auditConsistency(spark, sc, rawRows, auditDate));
            resultRows.addAll(auditTimeliness(spark, sc, rawRows, auditDate, gapThresholdMin));

            PgSink.writeBatch(params.pgUrl(), params.pgUser(), params.pgPassword(),
                    UPSERT_SQL, resultRows);
            System.out.println("[DataQualityJob] 数据质量稽核完成: date=" + auditDate
                    + ", 结果行=" + resultRows.size());
        }
    }

    /**
     * 拉取 TDengine sensor_data 目标日原始数据。
     *
     * @param params  作业参数
     * @param startTs 起始时间（含）
     * @param endTs   结束时间（不含）
     * @return Row 列表（deviceId/pipeType/ts/六指标）
     * @throws Exception JDBC 异常
     */
    private static List<Row> fetchRawData(SparkJobParams params, LocalDate auditDate)
            throws Exception {
        String sql = "SELECT device_id, pipe_type, ts, pressure, temperature, flow,"
                + " vibration, corrosion, leakage"
                + " FROM sensor_data"
                + " WHERE ts >= ? AND ts < ?";
        return JdbcSources.query(params.tdengineUrl(), params.tdengineUser(), params.tdenginePassword(),
                sql, rs -> {
                    String deviceId = rs.getString("device_id");
                    if (deviceId == null || deviceId.isBlank()) {
                        return null;
                    }
                    return RowFactory.create(
                            deviceId.trim(),
                            nullSafe(rs.getString("pipe_type"), "UNKNOWN"),
                            rs.getTimestamp("ts"),
                            getDoubleOrNull(rs, "pressure"),
                            getDoubleOrNull(rs, "temperature"),
                            getDoubleOrNull(rs, "flow"),
                            getDoubleOrNull(rs, "vibration"),
                            getDoubleOrNull(rs, "corrosion"),
                            getDoubleOrNull(rs, "leakage"));
                },
                java.sql.Timestamp.valueOf(auditDate.atStartOfDay()),
                java.sql.Timestamp.valueOf(auditDate.plusDays(1).atStartOfDay()));
    }

    /**
     * 完整性稽核：每设备实际采样点数 vs 期望点数。
     */
    private static List<Object[]> auditCompleteness(SparkSession spark, JavaSparkContext sc,
                                                     List<Row> rawRows, LocalDate auditDate,
                                                     int expectedPerDay) {
        StructType schema = new StructType()
                .add("deviceId", DataTypes.StringType)
                .add("pipeType", DataTypes.StringType);
        JavaRDD<Row> rdd = sc.parallelize(rawRows);
        Dataset<Row> df = spark.createDataFrame(rdd, schema);

        Dataset<Row> aggregated = df.groupBy("deviceId", "pipeType")
                .agg(count("*").alias("actualCount"));

        List<Object[]> results = new ArrayList<>();
        for (Row row : aggregated.collectAsList()) {
            String deviceId = row.getString(0);
            String pipeType = row.getString(1);
            long actual = row.getLong(2);
            long pass = (double) actual / expectedPerDay >= COMPLETENESS_PASS_RATE ? actual : 0;
            long fail = actual - pass;
            double passRate = actual > 0 ? (double) pass / actual : 0.0;
            String detail = String.format("expected=%d,actual=%d,rate=%.2f%%",
                    expectedPerDay, actual, (double) actual / expectedPerDay * 100);
            results.add(buildRow(auditDate, deviceId, pipeType, DIM_COMPLETENESS,
                    expectedPerDay, actual, pass, fail, passRate, detail));
        }
        return results;
    }

    /**
     * 有效性稽核：六类指标 NULL 与越界检查。
     */
    private static List<Object[]> auditValidity(SparkSession spark, JavaSparkContext sc,
                                                 List<Row> rawRows, LocalDate auditDate) {
        StructType schema = new StructType()
                .add("deviceId", DataTypes.StringType)
                .add("pipeType", DataTypes.StringType)
                .add("ts", DataTypes.TimestampType)
                .add("pressure", DataTypes.DoubleType)
                .add("temperature", DataTypes.DoubleType)
                .add("flow", DataTypes.DoubleType)
                .add("vibration", DataTypes.DoubleType)
                .add("corrosion", DataTypes.DoubleType)
                .add("leakage", DataTypes.DoubleType);
        JavaRDD<Row> rdd = sc.parallelize(rawRows);
        Dataset<Row> df = spark.createDataFrame(rdd, schema);

        // 逐指标统计越界+NULL 数（ts 在 index 2，指标从 index 3 开始）
        Dataset<Row> aggregated = df.groupBy("deviceId", "pipeType")
                .agg(count("*").alias("total"));

        // 对每个设备逐行统计失效条数（collect 后 driver 端计算，避免复杂 UDF）
        Map<String, long[]> deviceFailMap = new HashMap<>();
        Map<String, String> devicePipeMap = new HashMap<>();
        for (Row row : df.collectAsList()) {
            String deviceId = row.getString(0);
            String pipeType = row.getString(1);
            devicePipeMap.put(deviceId, pipeType);
            boolean invalid = false;
            for (int i = 0; i < METRICS.length; i++) {
                Double val = row.isNullAt(i + 3) ? null : row.getDouble(i + 3);
                if (val == null) {
                    invalid = true;
                    break;
                }
                double[] range = VALID_RANGES.get(METRICS[i]);
                if (val < range[0] || val > range[1]) {
                    invalid = true;
                    break;
                }
            }
            long[] counts = deviceFailMap.computeIfAbsent(deviceId, k -> new long[2]);
            counts[0]++; // total
            if (invalid) {
                counts[1]++; // fail
            }
        }

        List<Object[]> results = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : deviceFailMap.entrySet()) {
            String deviceId = entry.getKey();
            String pipeType = devicePipeMap.get(deviceId);
            long total = entry.getValue()[0];
            long fail = entry.getValue()[1];
            long pass = total - fail;
            double passRate = total > 0 ? (double) pass / total : 1.0;
            String detail = String.format("total=%d,invalid=%d(null/or-range)", total, fail);
            results.add(buildRow(auditDate, deviceId, pipeType, DIM_VALIDITY,
                    total, total, pass, fail, passRate, detail));
        }
        return results;
    }

    /**
     * 一致性稽核：重复时间戳检查。
     */
    private static List<Object[]> auditConsistency(SparkSession spark, JavaSparkContext sc,
                                                    List<Row> rawRows, LocalDate auditDate) {
        StructType schema = new StructType()
                .add("deviceId", DataTypes.StringType)
                .add("pipeType", DataTypes.StringType)
                .add("ts", DataTypes.TimestampType);
        JavaRDD<Row> rdd = sc.parallelize(rawRows);
        Dataset<Row> df = spark.createDataFrame(rdd, schema);

        Dataset<Row> aggregated = df.groupBy("deviceId", "pipeType")
                .agg(count("*").alias("total"),
                        countDistinct("ts").alias("distinctTs"));

        List<Object[]> results = new ArrayList<>();
        for (Row row : aggregated.collectAsList()) {
            String deviceId = row.getString(0);
            String pipeType = row.getString(1);
            long total = row.getLong(2);
            long distinctTs = row.getLong(3);
            long duplicates = total - distinctTs;
            double passRate = total > 0 ? (double) distinctTs / total : 1.0;
            String detail = duplicates > 0
                    ? String.format("total=%d,distinct_ts=%d,duplicates=%d", total, distinctTs, duplicates)
                    : "no_duplicates";
            results.add(buildRow(auditDate, deviceId, pipeType, DIM_CONSISTENCY,
                    total, total, distinctTs, duplicates, passRate, detail));
        }
        return results;
    }

    /**
     * 时效性稽核：相邻采样点时间间隔超过阈值的间隙检查。
     */
    private static List<Object[]> auditTimeliness(SparkSession spark, JavaSparkContext sc,
                                                   List<Row> rawRows, LocalDate auditDate,
                                                   int gapThresholdMin) {
        // 按设备分组，在 driver 端排序后检查间隙（设备数有限，driver 端可承受）
        Map<String, List<Long>> deviceTsMap = new HashMap<>();
        Map<String, String> devicePipeMap = new HashMap<>();
        long gapThresholdMs = gapThresholdMin * 60L * 1000L;

        for (Row row : rawRows) {
            String deviceId = row.getString(0);
            String pipeType = row.getString(1);
            java.sql.Timestamp ts = (java.sql.Timestamp) row.get(2);
            deviceTsMap.computeIfAbsent(deviceId, k -> new ArrayList<>()).add(ts.getTime());
            devicePipeMap.put(deviceId, pipeType);
        }

        List<Object[]> results = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : deviceTsMap.entrySet()) {
            String deviceId = entry.getKey();
            String pipeType = devicePipeMap.get(deviceId);
            List<Long> timestamps = entry.getValue();
            timestamps.sort(Long::compareTo);

            long total = timestamps.size();
            long gaps = 0;
            long maxGapMs = 0;
            for (int i = 1; i < timestamps.size(); i++) {
                long gap = timestamps.get(i) - timestamps.get(i - 1);
                if (gap > gapThresholdMs) {
                    gaps++;
                }
                maxGapMs = Math.max(maxGapMs, gap);
            }
            long pass = total - gaps;
            double passRate = total > 0 ? (double) pass / total : 1.0;
            String detail = gaps > 0
                    ? String.format("total=%d,gaps>%dmin=%d,max_gap=%dms",
                    total, gapThresholdMin, gaps, maxGapMs)
                    : String.format("total=%d,no_gaps>%dmin,max_gap=%dms",
                    total, gapThresholdMin, maxGapMs);
            results.add(buildRow(auditDate, deviceId, pipeType, DIM_TIMELINESS,
                    total, total, pass, gaps, passRate, detail));
        }
        return results;
    }

    /**
     * 构建稽核结果行。
     */
    private static Object[] buildRow(LocalDate auditDate, String deviceId, String pipeType,
                                      String dimension, long expected, long actual,
                                      long pass, long fail, double passRate, String detail) {
        // ID 使用 long 算术 hashCode 绝对值（幂等 upsert 以唯一索引为锚，ID 仅作占位）
        long hash = ((long) auditDate.hashCode() * 31 + deviceId.hashCode()) * 31 + dimension.hashCode();
        long id = Math.abs(hash % (Long.MAX_VALUE / 1000)) * 1000L + Math.abs((long) deviceId.hashCode() % 1000);
        return new Object[]{id, java.sql.Date.valueOf(auditDate), deviceId, pipeType,
                dimension, expected, actual, pass, fail, passRate, detail};
    }

    /**
     * 空值安全读取 Double（TDengine NULL 字段返回 0.0f，需判断原始列是否为 NULL）。
     */
    private static Double getDoubleOrNull(java.sql.ResultSet rs, String column) {
        try {
            double val = rs.getDouble(column);
            if (rs.wasNull()) {
                return null;
            }
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 空值兜底。
     */
    private static String nullSafe(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
