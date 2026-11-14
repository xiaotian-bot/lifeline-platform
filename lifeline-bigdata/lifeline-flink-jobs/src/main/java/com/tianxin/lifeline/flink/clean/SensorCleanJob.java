package com.tianxin.lifeline.flink.clean;

import com.fasterxml.jackson.databind.JsonNode;
import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.flink.common.EnvFactory;
import com.tianxin.lifeline.flink.common.JobParams;
import com.tianxin.lifeline.flink.common.JsonUtils;
import com.tianxin.lifeline.flink.common.SensorMessageParser;
import com.tianxin.lifeline.flink.model.SensorMessage;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

/**
 * 作业一：传感数据清洗（pipe_sensor_raw → pipe_sensor_clean）。
 *
 * <p>清洗职责：
 * <ol>
 *     <li>格式兼容：单条 JSON 对象与压测模式 JSON Array（先数组后单条回退解析）；</li>
 *     <li>合法性校验：deviceId 空白或 ts 缺失/非正的数据直接跳过；</li>
 *     <li>去重：keyBy(deviceId) 后以 ts 为键的短期状态去重（TTL 60s，
 *         拦截 Mock/网关重试造成的短时重复上报；上游分区键即 deviceId，
 *         同设备数据天然有序落同一并行度）；</li>
 *     <li>pipeType 归一化：大小写不敏感对齐 {@link PipeType} 枚举编码
 *         （water/Water → WATER），无法识别的值原样保留（与
 *         monitor-data-service SensorIngestServiceImpl 的归一化决策一致）；</li>
 *     <li>输出：标准裸 JSON（字段结构与上游契约一致），分区键 deviceId。</li>
 * </ol>
 *
 * <p>提交示例：
 * {@code flink run -c com.tianxin.lifeline.flink.clean.SensorCleanJob
 * lifeline-flink-jobs.jar --kafka kafka:9092 --parallelism 4}
 */
public class SensorCleanJob {

    /** 消费者组（与 Spring 服务消费组区分，独立位点） */
    private static final String GROUP_ID = "lifeline-flink-sensor-clean";

    /** 去重状态 TTL：60 秒短期窗口 */
    private static final long DEDUP_TTL_SECONDS = 60L;

    /**
     * 作业入口。
     *
     * @param args 启动参数（见 {@link JobParams}）
     * @throws Exception 作业构建/提交异常
     */
    public static void main(String[] args) throws Exception {
        JobParams params = JobParams.parse(args);
        StreamExecutionEnvironment env = EnvFactory.create(params);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(params.kafkaBootstrap())
                .setTopics(TopicConstants.PIPE_SENSOR_RAW)
                .setGroupId(GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> cleanStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-pipe_sensor_raw")
                .keyBy(SensorCleanJob::extractDeviceId)
                .flatMap(new CleanFunction())
                .name("sensor-clean");

        cleanStream.sinkTo(buildSink(params))
                .name("kafka-pipe_sensor_clean");

        env.execute("lifeline-sensor-clean-job");
    }

    /**
     * 从裸 JSON 中提取 deviceId 作为 keyBy 键（轻量 JsonNode 解析，
     * 避免完整反序列化；提取失败退化为空键）。
     *
     * @param payload 消息体
     * @return deviceId，失败返回空字符串
     */
    private static String extractDeviceId(String payload) {
        try {
            JsonNode root = JsonUtils.sensorMapper().readTree(payload);
            // 压测模式数组消息取首条 deviceId（同批通常同设备，发送端以首条为分区键）
            JsonNode target = root.isArray() && root.size() > 0 ? root.get(0) : root;
            JsonNode deviceId = target.get("deviceId");
            return deviceId == null || deviceId.isNull() ? "" : deviceId.asText();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 构建清洗输出 Kafka Sink（分区键 deviceId，事务 EXACTLY_ONCE 由
     * Checkpoint 协同保证；transaction.timeout.ms 需小于 Kafka broker 的
     * transaction.max.timeout.ms（默认 15min），此处取 5min）。
     *
     * @param params 作业参数
     * @return KafkaSink
     */
    private static KafkaSink<String> buildSink(JobParams params) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, params.kafkaBootstrap());
        producerProps.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 300_000);
        return KafkaSink.<String>builder()
                .setBootstrapServers(params.kafkaBootstrap())
                .setKafkaProducerConfig(producerProps)
                .setRecordSerializer(new CleanRecordSerializer())
                .build();
    }

    /**
     * 清洗算子（keyBy(deviceId) 之后执行）：解析 → 校验 → 去重 → 归一化 → 序列化输出。
     */
    public static class CleanFunction extends RichFlatMapFunction<String, String> {

        /** 去重状态：ts → 已见标记（当前分区已按 deviceId 分组），TTL 60 秒 */
        private transient MapState<Long, Boolean> seenState;

        @Override
        public void open(Configuration parameters) {
            MapStateDescriptor<Long, Boolean> descriptor =
                    new MapStateDescriptor<>("sensor-dedup", Types.LONG, Types.BOOLEAN);
            StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.seconds(DEDUP_TTL_SECONDS))
                    .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                    .cleanupInRocksdbCompactFilter(1000)
                    .build();
            descriptor.enableTimeToLive(ttlConfig);
            seenState = getRuntimeContext().getMapState(descriptor);
        }

        @Override
        public void flatMap(String payload, Collector<String> out) {
            List<SensorMessage> messages = SensorMessageParser.parse(payload);
            for (SensorMessage message : messages) {
                if (!SensorMessageParser.isValid(message)) {
                    // 非法数据（deviceId/ts 缺失）跳过，绝不中断作业
                    continue;
                }
                try {
                    if (Boolean.TRUE.equals(seenState.get(message.getTs()))) {
                        // 60 秒窗口内 deviceId + ts 重复上报，去重丢弃
                        continue;
                    }
                    seenState.put(message.getTs(), Boolean.TRUE);
                } catch (Exception e) {
                    // 状态访问异常不阻断清洗，放行走下游（下游幂等兜底）
                }
                normalizePipeType(message);
                try {
                    out.collect(JsonUtils.sensorMapper().writeValueAsString(message));
                } catch (Exception e) {
                    // 序列化失败仅跳过该条
                }
            }
        }

        /**
         * pipeType 归一化：大小写不敏感对齐 PipeType 枚举编码，未知值原样保留。
         *
         * @param message 传感数据（就地修改）
         */
        private void normalizePipeType(SensorMessage message) {
            String raw = message.getPipeType();
            if (raw == null || raw.isBlank()) {
                return;
            }
            PipeType type = PipeType.fromCode(raw.trim());
            message.setPipeType(type == null ? raw : type.getCode());
        }
    }

    /**
     * 清洗输出序列化器：key=deviceId，value=标准 JSON。
     */
    public static class CleanRecordSerializer
            implements KafkaRecordSerializationSchema<String> {

        @Override
        public ProducerRecord<byte[], byte[]> serialize(String element,
                                                        KafkaSinkContext context,
                                                        Long timestamp) {
            byte[] key = null;
            try {
                SensorMessage message = JsonUtils.sensorMapper().readValue(element, SensorMessage.class);
                if (message != null && message.getDeviceId() != null) {
                    key = message.getDeviceId().getBytes(StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
                // 解析失败仅退化为无键消息
            }
            return new ProducerRecord<>(TopicConstants.PIPE_SENSOR_CLEAN, key,
                    element.getBytes(StandardCharsets.UTF_8));
        }
    }
}
