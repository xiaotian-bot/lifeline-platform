package com.tianxin.lifeline.flink.feature;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.flink.common.EnvFactory;
import com.tianxin.lifeline.flink.common.JobParams;
import com.tianxin.lifeline.flink.common.SensorMessageParser;
import com.tianxin.lifeline.flink.model.SensorMessage;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 作业三：滑动窗口特征计算（pipe_sensor_clean → TDengine sensor_feature）。
 *
 * <p>窗口规格（事件时间语义，Watermark 容忍 5s 乱序）：
 * <table>
 *     <tr><th>粒度</th><th>窗口大小</th><th>滑动步长</th></tr>
 *     <tr><td>1m</td><td>1 分钟</td><td>30 秒</td></tr>
 *     <tr><td>5m</td><td>5 分钟</td><td>1 分钟</td></tr>
 *     <tr><td>1h</td><td>60 分钟</td><td>10 分钟</td></tr>
 * </table>
 *
 * <p>每个窗口对 6 类指标（pressure/temperature/flow/vibration/corrosion/leakage）
 * 计算 4 类统计特征：avg 均值 / peak 峰值 / std 标准差 / rate_of_change 变化率
 * （(末值-首值)/窗口秒数），连同样本数与窗口起止写入 sensor_feature，
 * TAGS 记录 device_id / pipe_type / sensor_type / interval_type。
 *
 * <p>提交示例：
 * {@code flink run -c com.tianxin.lifeline.flink.feature.FeatureWindowJob
 * lifeline-flink-jobs.jar --kafka kafka:9092 --tdengine-url jdbc:TAOS-RS://tdengine:6041/pipe_ts}
 */
public class FeatureWindowJob {

    /** 消费者组 */
    private static final String GROUP_ID = "lifeline-flink-feature-window";

    /** Watermark 乱序容忍（毫秒） */
    private static final long OUT_OF_ORDERNESS_MS = 5_000L;

    /**
     * 作业入口。
     *
     * @param args 启动参数（见 {@link JobParams}）
     * @throws Exception 作业构建/提交异常
     */
    public static void main(String[] args) throws Exception {
        JobParams params = JobParams.parse(args);
        StreamExecutionEnvironment env = EnvFactory.create(params);

        org.apache.flink.connector.kafka.source.KafkaSource<String> source =
                org.apache.flink.connector.kafka.source.KafkaSource.<String>builder()
                        .setBootstrapServers(params.kafkaBootstrap())
                        .setTopics(TopicConstants.PIPE_SENSOR_CLEAN)
                        .setGroupId(GROUP_ID)
                        .setStartingOffsets(
                                org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer.latest())
                        .setValueOnlyDeserializer(new SimpleStringSchema())
                        .build();

        // 解析清洗数据并分配事件时间水印
        DataStream<SensorMessage> sensorStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-pipe_sensor_clean")
                .flatMap(new ParseFunction())
                .name("parse-clean-json")
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<SensorMessage>forBoundedOutOfOrderness(
                                        Duration.ofMillis(OUT_OF_ORDERNESS_MS))
                                .withTimestampAssigner(
                                        (SerializableTimestampAssigner<SensorMessage>) (message, ts) ->
                                                message.getTs() == null ? 0L : message.getTs()))
                .name("event-time-watermark");

        // 三种窗口粒度并行计算后合并写出
        DataStream<FeatureRow> features = buildWindowStream(sensorStream, Time.minutes(1), Time.seconds(30), "1m")
                .union(buildWindowStream(sensorStream, Time.minutes(5), Time.minutes(1), "5m"),
                        buildWindowStream(sensorStream, Time.hours(1), Time.minutes(10), "1h"));

        features.addSink(new FeatureTDengineSink(
                        params.get("tdengine-url", "jdbc:TAOS-RS://tdengine:6041/pipe_ts"),
                        params.get("tdengine-user", "root"),
                        params.get("tdengine-password", "taosdata")))
                .name("tdengine-sensor_feature-sink");

        env.execute("lifeline-feature-window-job");
    }

    /**
     * 构建单一粒度的滑动窗口特征流。
     *
     * @param stream       传感数据流
     * @param size         窗口大小
     * @param slide        滑动步长
     * @param intervalType 粒度标签（1m/5m/1h）
     * @return 特征行流
     */
    private static DataStream<FeatureRow> buildWindowStream(DataStream<SensorMessage> stream,
                                                            Time size, Time slide, String intervalType) {
        return stream
                .keyBy(SensorMessage::getDeviceId)
                .window(SlidingEventTimeWindows.of(size, slide))
                .process(new FeatureComputeFunction(intervalType))
                .name("feature-window-" + intervalType);
    }

    /**
     * 清洗数据解析算子：JSON → SensorMessage（过滤非法数据）。
     */
    public static class ParseFunction
            extends org.apache.flink.api.common.functions.RichFlatMapFunction<String, SensorMessage> {

        @Override
        public void flatMap(String payload, Collector<SensorMessage> out) {
            for (SensorMessage message : SensorMessageParser.parse(payload)) {
                if (SensorMessageParser.isValid(message)) {
                    out.collect(message);
                }
            }
        }
    }

    /**
     * 窗口特征计算：单窗口内对 6 类指标统计 avg/peak/std/rate_of_change。
     *
     * <p>标签取值：pipe_type / sensor_type 取窗口内出现频次最高的值
     * （同设备窗口内通常恒定，频次统计仅作防御）。
     */
    public static class FeatureComputeFunction
            extends ProcessWindowFunction<SensorMessage, FeatureRow, String, TimeWindow> {

        private static final Logger log = LoggerFactory.getLogger(FeatureComputeFunction.class);

        /** 窗口粒度标签 */
        private final String intervalType;

        /**
         * 构造特征计算函数。
         *
         * @param intervalType 窗口粒度标签（1m/5m/1h）
         */
        public FeatureComputeFunction(String intervalType) {
            this.intervalType = intervalType;
        }

        @Override
        public void process(String deviceId, Context context, Iterable<SensorMessage> elements,
                            Collector<FeatureRow> out) {
            String[] metrics = SensorMessage.METRICS;
            int n = metrics.length;
            long[] count = new long[n];
            double[] sum = new double[n];
            double[] sumSq = new double[n];
            double[] max = new double[n];
            double[] firstValue = new double[n];
            long[] firstTs = new long[n];
            double[] lastValue = new double[n];
            long[] lastTs = new long[n];
            boolean[] hasValue = new boolean[n];
            for (int i = 0; i < n; i++) {
                max[i] = Double.NEGATIVE_INFINITY;
                firstTs[i] = Long.MAX_VALUE;
                lastTs[i] = Long.MIN_VALUE;
            }

            Map<String, Integer> pipeTypeFreq = new HashMap<>();
            Map<String, Integer> sensorTypeFreq = new HashMap<>();
            long totalSamples = 0;

            for (SensorMessage message : elements) {
                totalSamples++;
                accumulateTag(pipeTypeFreq, message.getPipeType());
                accumulateTag(sensorTypeFreq, message.getSensorType());
                long ts = message.getTs();
                for (int i = 0; i < n; i++) {
                    Double value = message.metricValue(metrics[i]);
                    if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
                        continue;
                    }
                    count[i]++;
                    sum[i] += value;
                    sumSq[i] += value * value;
                    if (value > max[i]) {
                        max[i] = value;
                    }
                    if (ts <= firstTs[i]) {
                        firstTs[i] = ts;
                        firstValue[i] = value;
                    }
                    if (ts >= lastTs[i]) {
                        lastTs[i] = ts;
                        lastValue[i] = value;
                    }
                    hasValue[i] = true;
                }
            }

            if (totalSamples == 0) {
                return;
            }
            TimeWindow window = context.window();
            long windowEnd = window.getEnd();
            long windowStart = window.getStart();
            double windowSeconds = Math.max(1.0, (windowEnd - windowStart) / 1000.0);
            String dominantPipeType = dominant(pipeTypeFreq);
            String dominantSensorType = dominant(sensorTypeFreq);

            for (int i = 0; i < n; i++) {
                if (!hasValue[i] || count[i] == 0) {
                    continue;
                }
                double avg = sum[i] / count[i];
                double variance = Math.max(0.0, sumSq[i] / count[i] - avg * avg);
                double std = Math.sqrt(variance);
                double rateOfChange = count[i] > 1
                        ? (lastValue[i] - firstValue[i]) / windowSeconds
                        : 0.0;
                out.collect(new FeatureRow(windowEnd, metrics[i], avg, max[i], std, rateOfChange,
                        (int) count[i], windowStart, windowEnd,
                        deviceId, dominantPipeType, dominantSensorType, intervalType));
            }
            if (log.isTraceEnabled()) {
                log.trace("特征窗口完成: deviceId={}, interval={}, samples={}",
                        deviceId, intervalType, totalSamples);
            }
        }

        /**
         * 频次累计（null/空白标签忽略）。
         */
        private void accumulateTag(Map<String, Integer> freq, String tag) {
            if (tag == null || tag.isBlank()) {
                return;
            }
            freq.merge(tag, 1, Integer::sum);
        }

        /**
         * 取频次最高的标签，空表返回 null。
         */
        private String dominant(Map<String, Integer> freq) {
            String best = null;
            int bestCount = 0;
            for (Map.Entry<String, Integer> entry : freq.entrySet()) {
                if (entry.getValue() > bestCount) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return best;
        }
    }
}
