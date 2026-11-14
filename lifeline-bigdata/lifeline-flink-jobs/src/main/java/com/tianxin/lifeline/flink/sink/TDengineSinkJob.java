package com.tianxin.lifeline.flink.sink;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.flink.common.EnvFactory;
import com.tianxin.lifeline.flink.common.JobParams;
import com.tianxin.lifeline.flink.model.SensorMessage;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.util.List;

/**
 * 作业二：清洗数据入库 TDengine（pipe_sensor_clean → sensor_data 超级表）。
 *
 * <p>链路：KafkaSource → keyBy(deviceId) → 攒批（2000 条 / 500ms 先到先触发，
 * 自定义 KeyedProcessFunction + ProcessingTime Timer）→ TDengine JDBC 批量写入。
 *
 * <p>写入语句与 monitor-data-service TDengineBatchWriter 完全同构：
 * {@code INSERT INTO t_{deviceId} USING sensor_data TAGS(...) VALUES(...)}，
 * 子表由 USING 子句自动创建，子表命名规则一致（t_dev_gas_001），
 * 双写切换期间两条链路写入同一批子表，便于对账。
 *
 * <p>连接方式参照 monitor-data-service TDengineConfig：TAOS-RS RESTful
 * （jdbc:TAOS-RS://host:6041/pipe_ts，REST 服务端口默认 6041）。
 *
 * <p>提交示例：
 * {@code flink run -c com.tianxin.lifeline.flink.sink.TDengineSinkJob
 * lifeline-flink-jobs.jar --kafka kafka:9092 --tdengine-url jdbc:TAOS-RS://tdengine:6041/pipe_ts}
 */
public class TDengineSinkJob {

    /** 消费者组 */
    private static final String GROUP_ID = "lifeline-flink-tdengine-sink";

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
                .setTopics(TopicConstants.PIPE_SENSOR_CLEAN)
                .setGroupId(GROUP_ID)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<List<SensorMessage>> batchStream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-pipe_sensor_clean")
                .keyBy(TDengineSinkJob::extractDeviceId)
                .process(new SensorBatchProcessFunction(2000, 500L))
                .name("batch-2000-500ms");

        batchStream.addSink(new TDengineBatchSink(
                        params.get("tdengine-url", "jdbc:TAOS-RS://tdengine:6041/pipe_ts"),
                        params.get("tdengine-user", "root"),
                        params.get("tdengine-password", "taosdata")))
                .name("tdengine-sensor_data-sink");

        env.execute("lifeline-tdengine-sink-job");
    }

    /**
     * 从清洗后 JSON 中提取 deviceId 作为 keyBy 键（同设备数据聚合至同一并行度攒批）。
     *
     * @param payload 清洗后消息体（标准单条 JSON）
     * @return deviceId，失败返回空字符串
     */
    private static String extractDeviceId(String payload) {
        try {
            SensorMessage message = com.tianxin.lifeline.flink.common.JsonUtils.sensorMapper()
                    .readValue(payload, SensorMessage.class);
            return message != null && message.getDeviceId() != null ? message.getDeviceId() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
