package com.tianxin.lifeline.spark.statistics;

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
import java.util.List;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.sum;

/**
 * 作业一：日统计（DailyStatisticsJob）。
 *
 * <p>统计目标日（--date，默认昨天）的管网监测概况，写 PG {@code stat_daily}：
 * <ol>
 *     <li>指标均值/峰值/标准差：优先读 TDengine {@code sensor_feature} 预聚合表
 *         （FeatureWindowJob 产出的 1h 粒度特征），按 pipe_type × metric 维度
 *         以 sample_count 加权聚合；该维度 area_id 记为 ALL。</li>
 *     <li>数据量：TDengine {@code sensor_data} 原始表按 pipe_type × area_id
 *         统计样本条数（metric_name=DATA_COUNT）。</li>
 * </ol>
 *
 * <p>实现方式：driver 端 JDBC 拉取日级数据 → parallelize 为 DataFrame →
 * Spark 聚合 → driver 端 ON CONFLICT 幂等 upsert（作业可重复执行）。
 *
 * <p>提交示例：
 * {@code spark-submit --class com.tianxin.lifeline.spark.statistics.DailyStatisticsJob
 * lifeline-spark-jobs.jar --date 2026-08-17
 * --tdengine-url jdbc:TAOS://localhost:6030/pipe_ts
 * --pg-url jdbc:postgresql://localhost:5432/lifeline}
 */
public final class DailyStatisticsJob {

    /** 六类监测指标（与 sensor_data 超级表列对齐） */
    public static final String[] METRICS =
            {"pressure", "temperature", "flow", "vibration", "corrosion", "leakage"};

    /** 数据量统计行使用的指标名占位 */
    private static final String DATA_COUNT_METRIC = "DATA_COUNT";

    /** 特征维度无区域归属时的 area_id 占位 */
    private static final String AREA_ALL = "ALL";

    /** stat_daily 幂等 upsert SQL */
    private static final String UPSERT_SQL = """
            INSERT INTO stat_daily (stat_date, pipe_type, area_id, metric_name,
                                      sample_count, avg_value, peak_value, std_value)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (stat_date, pipe_type, area_id, metric_name) DO UPDATE SET
                sample_count = EXCLUDED.sample_count,
                avg_value    = EXCLUDED.avg_value,
                peak_value   = EXCLUDED.peak_value,
                std_value    = EXCLUDED.std_value,
                updated_at   = CURRENT_TIMESTAMP
            """;

    private DailyStatisticsJob() {
    }

    /**
     * 作业入口。
     *
     * @param args 启动参数（见 {@link SparkJobParams}）
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        SparkJobParams params = SparkJobParams.parse(args);
        LocalDate statDate = params.date();
        String startTs = statDate + " 00:00:00.000";
        String endTs = statDate.plusDays(1) + " 00:00:00.000";

        try (SparkSession spark = SparkSessionFactory.create("lifeline-daily-statistics-job", params)) {
            JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

            // 1. 特征预聚合表拉取（pipe_type × metric 维度均值/峰值/标准差）
            List<Row> featureRows = fetchFeatureRows(params, startTs, endTs);
            // 2. 原始表拉取（pipe_type × area 维度数据量）
            List<Row> countRows = fetchDataCountRows(params, startTs, endTs);

            List<Object[]> resultRows = new ArrayList<>();
            resultRows.addAll(aggregateFeatures(spark, sc, featureRows, statDate));
            resultRows.addAll(aggregateDataCount(spark, sc, countRows, statDate));

            PgSink.writeBatch(params.pgUrl(), params.pgUser(), params.pgPassword(),
                    UPSERT_SQL, resultRows);
            System.out.println("[DailyStatisticsJob] 日统计完成: date=" + statDate
                    + ", 结果行=" + resultRows.size());
        }
    }

    /**
     * 拉取 TDengine sensor_feature 1h 粒度特征行（优先数据源）。
     *
     * @param params  作业参数
     * @param startTs 起始时间（含）
     * @param endTs   结束时间（不含）
     * @return Spark Row 列表（pipeType/metricName/avgValue/peakValue/stdValue/sampleCount）
     * @throws Exception JDBC 异常
     */
    private static List<Row> fetchFeatureRows(SparkJobParams params, String startTs, String endTs)
            throws Exception {
        String sql = "SELECT pipe_type, metric_name, avg_value, peak_value, std_value, sample_count"
                + " FROM sensor_feature"
                + " WHERE interval_type = '1h' AND ts >= ? AND ts < ?";
        return JdbcSources.query(params.tdengineUrl(), params.tdengineUser(), params.tdenginePassword(),
                sql, rs -> {
                    String pipeType = rs.getString("pipe_type");
                    String metricName = rs.getString("metric_name");
                    if (pipeType == null || metricName == null) {
                        return null;
                    }
                    return RowFactory.create(pipeType, metricName,
                            rs.getDouble("avg_value"), rs.getDouble("peak_value"),
                            rs.getDouble("std_value"), (long) rs.getInt("sample_count"));
                },
                java.sql.Timestamp.valueOf(java.time.LocalDate.parse(startTs.substring(0, 10)).atStartOfDay()),
                java.sql.Timestamp.valueOf(java.time.LocalDate.parse(endTs.substring(0, 10)).atStartOfDay()));
    }

    /**
     * 拉取 TDengine sensor_data 原始行（仅取 pipe_type/area_id 两个标签列做数据量统计）。
     *
     * @param params  作业参数
     * @param startTs 起始时间（含）
     * @param endTs   结束时间（不含）
     * @return Spark Row 列表（pipeType/areaId）
     * @throws Exception JDBC 异常
     */
    private static List<Row> fetchDataCountRows(SparkJobParams params, String startTs, String endTs)
            throws Exception {
        String sql = "SELECT pipe_type, area_id FROM sensor_data"
                + " WHERE ts >= ? AND ts < ?";
        return JdbcSources.query(params.tdengineUrl(), params.tdengineUser(), params.tdenginePassword(),
                sql, rs -> RowFactory.create(
                        nullSafe(rs.getString("pipe_type"), "UNKNOWN"),
                        nullSafe(rs.getString("area_id"), "UNKNOWN")),
                java.sql.Timestamp.valueOf(java.time.LocalDate.parse(startTs.substring(0, 10)).atStartOfDay()),
                java.sql.Timestamp.valueOf(java.time.LocalDate.parse(endTs.substring(0, 10)).atStartOfDay()));
    }

    /**
     * Spark 聚合特征行：pipe_type × metric 维度，均值按 sample_count 加权，
     * 峰值取 max，标准差取均值（窗口 std 的二次近似，日级报表可接受）。
     *
     * @param spark      SparkSession
     * @param sc         JavaSparkContext
     * @param featureRows 特征行
     * @param statDate   统计日
     * @return stat_daily 参数行列表
     */
    private static List<Object[]> aggregateFeatures(SparkSession spark, JavaSparkContext sc,
                                                    List<Row> featureRows, LocalDate statDate) {
        List<Object[]> rows = new ArrayList<>();
        if (featureRows.isEmpty()) {
            return rows;
        }
        StructType schema = new StructType()
                .add("pipeType", DataTypes.StringType)
                .add("metricName", DataTypes.StringType)
                .add("avgValue", DataTypes.DoubleType)
                .add("peakValue", DataTypes.DoubleType)
                .add("stdValue", DataTypes.DoubleType)
                .add("sampleCount", DataTypes.LongType);
        JavaRDD<Row> rdd = sc.parallelize(featureRows);
        Dataset<Row> df = spark.createDataFrame(rdd, schema);

        // 加权均值：sum(avg * sample) / sum(sample)
        Dataset<Row> aggregated = df
                .withColumn("weightedAvg", col("avgValue").multiply(col("sampleCount")))
                .groupBy("pipeType", "metricName")
                .agg(sum("weightedAvg").alias("sumWeightedAvg"),
                        sum("sampleCount").alias("totalSamples"),
                        max("peakValue").alias("peakValue"),
                        avg("stdValue").alias("stdValue"));

        for (Row row : aggregated.collectAsList()) {
            long totalSamples = row.getAs("totalSamples");
            double sumWeightedAvg = row.getAs("sumWeightedAvg");
            double avgValue = totalSamples > 0 ? sumWeightedAvg / totalSamples : 0.0;
            rows.add(new Object[]{java.sql.Date.valueOf(statDate),
                    row.getString(0), AREA_ALL, row.getString(1),
                    totalSamples, avgValue, row.getDouble(2), row.getDouble(3)});
        }
        return rows;
    }

    /**
     * Spark 聚合数据量行：pipe_type × area_id 维度样本条数。
     *
     * @param spark     SparkSession
     * @param sc        JavaSparkContext
     * @param countRows 数据量行
     * @param statDate  统计日
     * @return stat_daily 参数行列表（metric_name=DATA_COUNT，均值/峰值/标准差置空）
     */
    private static List<Object[]> aggregateDataCount(SparkSession spark, JavaSparkContext sc,
                                                     List<Row> countRows, LocalDate statDate) {
        List<Object[]> rows = new ArrayList<>();
        if (countRows.isEmpty()) {
            return rows;
        }
        StructType schema = new StructType()
                .add("pipeType", DataTypes.StringType)
                .add("areaId", DataTypes.StringType);
        JavaRDD<Row> rdd = sc.parallelize(countRows);
        Dataset<Row> df = spark.createDataFrame(rdd, schema);

        Dataset<Row> aggregated = df.groupBy("pipeType", "areaId")
                .agg(count("*").alias("sampleCount"));
        for (Row row : aggregated.collectAsList()) {
            rows.add(new Object[]{java.sql.Date.valueOf(statDate),
                    row.getString(0), row.getString(1), DATA_COUNT_METRIC,
                    row.getLong(2), null, null, null});
        }
        return rows;
    }

    /**
     * 空值兜底。
     *
     * @param value        原始值
     * @param defaultValue 兜底值
     * @return 非空原值，否则兜底值
     */
    private static String nullSafe(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
