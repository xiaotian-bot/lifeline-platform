package com.tianxin.lifeline.spark.trend;

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

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;

/**
 * 作业二：隐患趋势统计（WeeklyTrendJob）。
 *
 * <p>聚合 PG {@code warning_event}（deleted=0）在 --start-date ~ --end-date
 * 范围内按日/周两个粒度的隐患数量，并拆分等级分布（LEVEL）与类型分布（TYPE），
 * 结果幂等写入 PG {@code stat_warning_trend}：
 * <ul>
 *     <li>period_type=DAY：以事件日期为 period_start 的每日统计；</li>
 *     <li>period_type=WEEK：以事件所在周的周一为 period_start 的每周统计。</li>
 * </ul>
 * dimension_type 取值 TOTAL（总量）/ LEVEL（按 warning_level 分布）/
 * TYPE（按 warning_type 分布），dimension_value 为 ALL / 等级数字串 / 类型编码。
 *
 * <p>实现方式：driver 端 JDBC 拉取事件行（仅 created_at/warning_level/warning_type
 * 三列）→ parallelize 为 DataFrame → Spark 多维聚合 → driver 端
 * ON CONFLICT 幂等 upsert。
 *
 * <p>提交示例：
 * {@code spark-submit --class com.tianxin.lifeline.spark.trend.WeeklyTrendJob
 * lifeline-spark-jobs.jar --start-date 2026-08-01 --end-date 2026-08-17}
 */
public final class WeeklyTrendJob {

    /** 周期类型：日 */
    private static final String PERIOD_DAY = "DAY";

    /** 周期类型：周 */
    private static final String PERIOD_WEEK = "WEEK";

    /** 维度类型：总量 */
    private static final String DIM_TOTAL = "TOTAL";

    /** 维度类型：预警等级分布 */
    private static final String DIM_LEVEL = "LEVEL";

    /** 维度类型：预警类型分布 */
    private static final String DIM_TYPE = "TYPE";

    /** TOTAL 维度的 dimension_value 占位 */
    private static final String VALUE_ALL = "ALL";

    /** stat_warning_trend 幂等 upsert SQL */
    private static final String UPSERT_SQL = """
            INSERT INTO stat_warning_trend (period_type, period_start, dimension_type,
                                            dimension_value, warning_count)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (period_type, period_start, dimension_type, dimension_value) DO UPDATE SET
                warning_count = EXCLUDED.warning_count,
                updated_at    = CURRENT_TIMESTAMP
            """;

    private WeeklyTrendJob() {
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

        try (SparkSession spark = SparkSessionFactory.create("lifeline-weekly-trend-job", params)) {
            JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

            List<Row> events = fetchEvents(params, startDate, endDate);
            System.out.println("[WeeklyTrendJob] 拉取预警事件 " + events.size()
                    + " 条, 范围=[" + startDate + ", " + endDate + "]");

            List<Object[]> resultRows = new ArrayList<>();
            if (!events.isEmpty()) {
                StructType schema = new StructType()
                        .add("eventDate", DataTypes.DateType)
                        .add("warningLevel", DataTypes.IntegerType)
                        .add("warningType", DataTypes.StringType);
                JavaRDD<Row> rdd = sc.parallelize(events);
                Dataset<Row> df = spark.createDataFrame(rdd, schema);
                resultRows.addAll(aggregate(spark, df, startDate, endDate));
            }

            PgSink.writeBatch(params.pgUrl(), params.pgUser(), params.pgPassword(),
                    UPSERT_SQL, resultRows);
            System.out.println("[WeeklyTrendJob] 趋势统计完成, 结果行=" + resultRows.size());
        }
    }

    /**
     * 拉取范围内预警事件（deleted=0，status 不限——趋势统计含误报由业务侧自行过滤展示）。
     *
     * @param params    作业参数
     * @param startDate 起始日（含）
     * @param endDate   结束日（含）
     * @return Spark Row 列表（eventDate/warningLevel/warningType）
     * @throws Exception JDBC 异常
     */
    private static List<Row> fetchEvents(SparkJobParams params, LocalDate startDate, LocalDate endDate)
            throws Exception {
        String sql = "SELECT created_at, warning_level, warning_type FROM warning_event"
                + " WHERE deleted = 0 AND created_at >= ? AND created_at < ?";
        return JdbcSources.query(params.pgUrl(), params.pgUser(), params.pgPassword(),
                sql, rs -> {
                    java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt == null) {
                        return null;
                    }
                    return RowFactory.create(createdAt.toLocalDateTime().toLocalDate(),
                            rs.getInt("warning_level"),
                            nullSafe(rs.getString("warning_type"), "UNKNOWN"));
                },
                java.sql.Timestamp.valueOf(startDate.atStartOfDay()),
                java.sql.Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
    }

    /**
     * 多维聚合：DAY/WEEK 两粒度 × TOTAL/LEVEL/TYPE 三维。
     *
     * @param spark     SparkSession
     * @param df        事件 DataFrame
     * @param startDate 范围起始日
     * @param endDate   范围结束日
     * @return stat_warning_trend 参数行列表
     */
    private static List<Object[]> aggregate(SparkSession spark, Dataset<Row> df,
                                            LocalDate startDate, LocalDate endDate) {
        List<Object[]> rows = new ArrayList<>();
        // 周起始日：事件所在周的周一（dayofweek：周日=1，偏移 (dow+5)%7 即距周一天数）
        Dataset<Row> dfWithPeriod = df
                .withColumn("dayStart", col("eventDate"))
                .withColumn("weekStartCol",
                        org.apache.spark.sql.functions.date_sub(
                                org.apache.spark.sql.functions.date_add(col("eventDate"), 1),
                                org.apache.spark.sql.functions.expr("pmod(dayofweek(eventDate) + 5, 7)")));

        // DAY × TOTAL
        collect(dfWithPeriod.groupBy("dayStart").agg(count("*").alias("cnt")),
                "dayStart", rows, PERIOD_DAY, DIM_TOTAL, VALUE_ALL);
        // DAY × LEVEL
        collect(dfWithPeriod.groupBy("dayStart", "warningLevel").agg(count("*").alias("cnt")),
                "dayStart", rows, PERIOD_DAY, DIM_LEVEL, null);
        // DAY × TYPE
        collect(dfWithPeriod.groupBy("dayStart", "warningType").agg(count("*").alias("cnt")),
                "dayStart", rows, PERIOD_DAY, DIM_TYPE, null);
        // WEEK × TOTAL
        collect(dfWithPeriod.groupBy("weekStartCol").agg(count("*").alias("cnt")),
                "weekStartCol", rows, PERIOD_WEEK, DIM_TOTAL, VALUE_ALL);
        // WEEK × LEVEL
        collect(dfWithPeriod.groupBy("weekStartCol", "warningLevel").agg(count("*").alias("cnt")),
                "weekStartCol", rows, PERIOD_WEEK, DIM_LEVEL, null);
        // WEEK × TYPE
        collect(dfWithPeriod.groupBy("weekStartCol", "warningType").agg(count("*").alias("cnt")),
                "weekStartCol", rows, PERIOD_WEEK, DIM_TYPE, null);
        return rows;
    }

    /**
     * 收集聚合结果到 upsert 参数行。
     *
     * @param aggregated    聚合 DataFrame（首列为周期起始日，尾列 cnt）
     * @param periodColumn  周期起始日列名
     * @param rows          结果收集容器
     * @param periodType    周期类型（DAY/WEEK）
     * @param dimensionType 维度类型（TOTAL/LEVEL/TYPE）
     * @param fixedValue    固定 dimension_value（TOTAL 时传 ALL；其他维度传 null 取第 2 列）
     */
    private static void collect(Dataset<Row> aggregated, String periodColumn, List<Object[]> rows,
                                String periodType, String dimensionType, String fixedValue) {
        int periodIndex = aggregated.schema().fieldIndex(periodColumn);
        boolean hasDimensionColumn = fixedValue == null;
        for (Row row : aggregated.collectAsList()) {
            LocalDate periodStart = row.getAs(periodIndex);
            String dimensionValue = fixedValue;
            if (hasDimensionColumn) {
                Object dimValue = row.get(row.fieldIndex(
                        dimensionType.equals(DIM_LEVEL) ? "warningLevel" : "warningType"));
                dimensionValue = dimValue == null ? "UNKNOWN" : String.valueOf(dimValue);
            }
            rows.add(new Object[]{periodType, java.sql.Date.valueOf(periodStart),
                    dimensionType, dimensionValue, row.getAs("cnt")});
        }
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
