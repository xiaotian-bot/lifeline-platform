package com.tianxin.lifeline.spark.training;

import com.tianxin.lifeline.spark.common.JdbcSources;
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
import java.util.Iterator;
import java.util.List;

/**
 * 作业三：训练数据集生成（TrainingDatasetJob）。
 *
 * <p>从 TDengine {@code sensor_data} 超级表拉取 --start-date ~ --end-date
 * 范围的原始监测数据，经抽样（--sample-ratio，默认 1.0 全量）后按
 * device_id 分组、时间升序滑动窗口（--window-size 默认 60 步、
 * --window-step 默认 30 步）展开为窗口化特征宽表，
 * 输出 CSV 或 Parquet 至 --output-dir（本地路径或 s3a:// MinIO 路径），
 * 供 TimesNet 等时序深度模型训练。
 *
 * <p>输出列：device_id / pipe_type / window_index / window_start_ts /
 * window_end_ts / seq_index（窗口内序号）及六类指标值，
 * 即「长序列窗口样本」组织方式，训练侧按 (device_id, window_index)
 * 分组即可还原时序张量。
 *
 * <p>MinIO（s3a）输出时需额外环境变量/参数：
 * --minio-endpoint（默认 http://localhost:9000）、
 * --minio-access-key（默认 minioadmin）、--minio-secret-key（默认 minioadmin123）。
 *
 * <p>提交示例：
 * {@code spark-submit --class com.tianxin.lifeline.spark.training.TrainingDatasetJob
 * lifeline-spark-jobs.jar --start-date 2026-08-01 --end-date 2026-08-14
 * --output-dir ./spark-output/training --format parquet --sample-ratio 0.5}
 */
public final class TrainingDatasetJob {

    /** 输出格式：csv */
    private static final String FORMAT_CSV = "csv";

    /** 输出格式：parquet */
    private static final String FORMAT_PARQUET = "parquet";

    /** 六类监测指标列（与 sensor_data 超级表列对齐） */
    private static final String[] METRICS =
            {"pressure", "temperature", "flow", "vibration", "corrosion", "leakage"};

    private TrainingDatasetJob() {
    }

    /**
     * 作业入口。
     *
     * @param args 启动参数（见 {@link SparkJobParams}）
     * @throws Exception 作业执行异常
     */
    public static void main(String[] args) throws Exception {
        SparkJobParams params = SparkJobParams.parse(args);
        LocalDate startDate = params.startDate();
        LocalDate endDate = params.endDate();
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("--end-date 不能早于 --start-date");
        }
        String outputDir = params.get("output-dir", "./spark-output/training-dataset");
        String format = params.get("format", FORMAT_PARQUET).trim().toLowerCase();
        if (!FORMAT_CSV.equals(format) && !FORMAT_PARQUET.equals(format)) {
            throw new IllegalArgumentException("--format 仅支持 csv / parquet, 实际=" + format);
        }
        double sampleRatio = params.getDouble("sample-ratio", 1.0);
        int windowSize = (int) params.getLong("window-size", 60);
        int windowStep = (int) params.getLong("window-step", 30);
        if (windowSize <= 0 || windowStep <= 0 || windowStep > windowSize) {
            throw new IllegalArgumentException("--window-size/--window-step 非法（需 0 < step <= size）");
        }

        try (SparkSession spark = SparkSessionFactory.create("lifeline-training-dataset-job", params)) {
            configureMinioIfNeeded(spark, params, outputDir);
            JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

            // 1. TDengine 拉取原始行
            List<Row> rawRows = fetchSensorRows(params, startDate, endDate);
            System.out.println("[TrainingDatasetJob] 拉取原始样本 " + rawRows.size()
                    + " 行, 范围=[" + startDate + ", " + endDate + "]");
            if (rawRows.isEmpty()) {
                System.out.println("[TrainingDatasetJob] 无数据, 跳过输出");
                return;
            }

            // 2. 抽样 + 窗口化展开
            StructType schema = buildSchema();
            JavaRDD<Row> rdd = sc.parallelize(rawRows);
            Dataset<Row> source = spark.createDataFrame(rdd, schema);
            if (sampleRatio > 0 && sampleRatio < 1.0) {
                source = source.sample(false, sampleRatio, 42L);
            }

            // 按设备+时间排序后，通过 mapPartitions 进行窗口化展开
            StructType outputSchema = buildOutputSchema();
            JavaRDD<Row> sortedRdd = source.orderBy("deviceId", "ts").toJavaRDD();
            JavaRDD<Row> windowedRdd = sortedRdd.mapPartitions(
                    (Iterator<Row> it) -> windowExpandAll(it, windowSize, windowStep));
            Dataset<Row> windows = spark.createDataFrame(windowedRdd, outputSchema);

            // 3. 输出
            if (FORMAT_CSV.equals(format)) {
                windows.coalesce(1).write().option("header", "true").mode("overwrite").csv(outputDir);
            } else {
                windows.write().mode("overwrite").parquet(outputDir);
            }
            System.out.println("[TrainingDatasetJob] 训练数据集输出完成: dir=" + outputDir
                    + ", format=" + format + ", sampleRatio=" + sampleRatio);
        }
    }

    /**
     * 拉取 TDengine sensor_data 原始监测行。
     *
     * @param params    作业参数
     * @param startDate 起始日（含）
     * @param endDate   结束日（含）
     * @return Spark Row 列表（deviceId/pipeType/ts/六指标）
     * @throws Exception JDBC 异常
     */
    private static List<Row> fetchSensorRows(SparkJobParams params, LocalDate startDate, LocalDate endDate)
            throws Exception {
        String sql = "SELECT device_id, pipe_type, ts, pressure, temperature, flow,"
                + " vibration, corrosion, leakage FROM sensor_data"
                + " WHERE ts >= ? AND ts < ?";
        return JdbcSources.query(params.tdengineUrl(), params.tdengineUser(), params.tdenginePassword(),
                sql, rs -> {
                    String deviceId = rs.getString("device_id");
                    if (deviceId == null || deviceId.isBlank()) {
                        return null;
                    }
                    return RowFactory.create(deviceId,
                            nullSafe(rs.getString("pipe_type"), "UNKNOWN"),
                            rs.getTimestamp("ts"),
                            rs.getDouble("pressure"), rs.getDouble("temperature"),
                            rs.getDouble("flow"), rs.getDouble("vibration"),
                            rs.getDouble("corrosion"), rs.getDouble("leakage"));
                },
                java.sql.Timestamp.valueOf(startDate.atStartOfDay()),
                java.sql.Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
    }

    /**
     * 分区级窗口展开：遍历已按 (deviceId, ts) 排序的分区迭代器，
     * 在设备边界处切分并执行滑动窗口展开。
     *
     * @param iterator   分区内已排序行迭代器
     * @param windowSize 窗口长度
     * @param windowStep 窗口步进
     * @return 窗口化特征行迭代器
     */
    private static Iterator<Row> windowExpandAll(Iterator<Row> iterator,
                                                  int windowSize, int windowStep) {
        // 按设备分组缓冲
        List<Row> buffer = new ArrayList<>();
        iterator.forEachRemaining(buffer::add);

        List<Row> outputs = new ArrayList<>();
        if (buffer.isEmpty()) {
            return outputs.iterator();
        }

        // 按 deviceId 切分后逐设备展开
        String currentDevice = null;
        List<Row> deviceBuffer = new ArrayList<>();
        for (Row row : buffer) {
            String deviceId = row.getString(0);
            if (!deviceId.equals(currentDevice)) {
                if (!deviceBuffer.isEmpty() && currentDevice != null) {
                    expandSingleDevice(currentDevice, deviceBuffer, windowSize, windowStep, outputs);
                }
                currentDevice = deviceId;
                deviceBuffer.clear();
            }
            deviceBuffer.add(row);
        }
        // 处理最后一个设备
        if (!deviceBuffer.isEmpty() && currentDevice != null) {
            expandSingleDevice(currentDevice, deviceBuffer, windowSize, windowStep, outputs);
        }
        return outputs.iterator();
    }

    /**
     * 单设备滑动窗口展开：按时间升序的样本序列切分为
     * window_size 长、window_step 步进的窗口样本。
     *
     * @param deviceId   设备 ID
     * @param rows       该设备时间升序行列表
     * @param windowSize 窗口长度
     * @param windowStep 窗口步进
     * @param outputs    输出收集列表
     */
    private static void expandSingleDevice(String deviceId, List<Row> rows,
                                            int windowSize, int windowStep, List<Row> outputs) {
        if (rows.size() < windowSize) {
            return;
        }
        int windowIndex = 0;
        for (int start = 0; start + windowSize <= rows.size(); start += windowStep, windowIndex++) {
            java.sql.Timestamp windowStart = rows.get(start).getAs("ts");
            java.sql.Timestamp windowEnd = rows.get(start + windowSize - 1).getAs("ts");
            String pipeType = rows.get(start).getString(1);
            for (int seq = 0; seq < windowSize; seq++) {
                Row raw = rows.get(start + seq);
                outputs.add(RowFactory.create(deviceId, pipeType, windowIndex,
                        windowStart, windowEnd, seq,
                        raw.getDouble(3), raw.getDouble(4), raw.getDouble(5),
                        raw.getDouble(6), raw.getDouble(7), raw.getDouble(8)));
            }
        }
    }

    /**
     * 输入 schema（与 fetchSensorRows 产出列对齐）。
     *
     * @return 输入 StructType
     */
    private static StructType buildSchema() {
        return new StructType()
                .add("deviceId", DataTypes.StringType)
                .add("pipeType", DataTypes.StringType)
                .add("ts", DataTypes.TimestampType)
                .add("pressure", DataTypes.DoubleType)
                .add("temperature", DataTypes.DoubleType)
                .add("flow", DataTypes.DoubleType)
                .add("vibration", DataTypes.DoubleType)
                .add("corrosion", DataTypes.DoubleType)
                .add("leakage", DataTypes.DoubleType);
    }

    /**
     * 输出 schema（窗口化特征宽表）。
     *
     * @return 输出 StructType
     */
    private static StructType buildOutputSchema() {
        StructType schema = new StructType()
                .add("device_id", DataTypes.StringType)
                .add("pipe_type", DataTypes.StringType)
                .add("window_index", DataTypes.IntegerType)
                .add("window_start_ts", DataTypes.TimestampType)
                .add("window_end_ts", DataTypes.TimestampType)
                .add("seq_index", DataTypes.IntegerType);
        for (String metric : METRICS) {
            schema = schema.add(metric, DataTypes.DoubleType);
        }
        return schema;
    }

    /**
     * s3a:// 输出目录时注入 MinIO 兼容的 Hadoop S3A 配置。
     *
     * @param spark     SparkSession
     * @param params    作业参数
     * @param outputDir 输出目录
     */
    private static void configureMinioIfNeeded(SparkSession spark, SparkJobParams params, String outputDir) {
        if (outputDir == null || !outputDir.toLowerCase().startsWith("s3a://")) {
            return;
        }
        spark.conf().set("spark.hadoop.fs.s3a.endpoint",
                params.get("minio-endpoint", "http://localhost:9000"));
        spark.conf().set("spark.hadoop.fs.s3a.access.key",
                params.get("minio-access-key", "minioadmin"));
        spark.conf().set("spark.hadoop.fs.s3a.secret.key",
                params.get("minio-secret-key", "minioadmin123"));
        spark.conf().set("spark.hadoop.fs.s3a.path.style.access", "true");
        spark.conf().set("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        spark.conf().set("spark.hadoop.fs.s3a.connection.ssl.enabled", "false");
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
