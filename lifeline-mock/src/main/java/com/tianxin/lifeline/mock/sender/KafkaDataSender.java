package com.tianxin.lifeline.mock.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.mock.config.MockProperties;
import com.tianxin.lifeline.mock.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 数据发送器。
 *
 * <p>将生成的传感数据上报至 Topic {@link TopicConstants#PIPE_SENSOR_RAW}：
 * <ul>
 *     <li>常规模式：每条数据一条 Kafka 消息，分区键为设备ID
 *         （保证同设备数据落入同分区、消费有序）；</li>
 *     <li>压测模式：每 100 条合并为单条 JSON Array 消息，
 *         降低消息条数、提升吞吐。</li>
 * </ul>
 */
@Slf4j
@Component
public class KafkaDataSender {

    /** 压测模式单条消息合并的数据条数 */
    public static final int STRESS_BATCH_SIZE = 100;

    /** 原始传感数据专用 KafkaTemplate */
    private final KafkaTemplate<String, String> rawKafkaTemplate;

    /** Mock 配置 */
    private final MockProperties properties;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造发送器。
     *
     * @param rawKafkaTemplate 原始数据 KafkaTemplate
     * @param properties       Mock 配置
     */
    public KafkaDataSender(@Qualifier("rawKafkaTemplate") KafkaTemplate<String, String> rawKafkaTemplate,
                           MockProperties properties) {
        this.rawKafkaTemplate = rawKafkaTemplate;
        this.properties = properties;
    }

    /**
     * 发送一批传感数据。
     *
     * @param batch 传感数据列表
     * @return 实际发送的数据条数
     */
    public int send(List<SensorData> batch) {
        if (batch == null || batch.isEmpty()) {
            return 0;
        }
        return properties.getStress().isMode() ? sendStress(batch) : sendNormal(batch);
    }

    /**
     * 常规模式：逐条发送。
     *
     * @param batch 传感数据列表
     * @return 发送条数
     */
    private int sendNormal(List<SensorData> batch) {
        int count = 0;
        for (SensorData data : batch) {
            try {
                rawKafkaTemplate.send(TopicConstants.PIPE_SENSOR_RAW,
                        data.getDeviceId(), objectMapper.writeValueAsString(data));
                count++;
            } catch (JsonProcessingException e) {
                log.error("传感数据序列化失败: deviceId={}", data.getDeviceId(), e);
            }
        }
        return count;
    }

    /**
     * 压测模式：每 {@link #STRESS_BATCH_SIZE} 条合并为单条 JSON Array 消息。
     *
     * @param batch 传感数据列表
     * @return 发送条数
     */
    private int sendStress(List<SensorData> batch) {
        int count = 0;
        for (int i = 0; i < batch.size(); i += STRESS_BATCH_SIZE) {
            List<SensorData> chunk = batch.subList(i, Math.min(i + STRESS_BATCH_SIZE, batch.size()));
            try {
                String key = chunk.get(0).getDeviceId();
                rawKafkaTemplate.send(TopicConstants.PIPE_SENSOR_RAW,
                        key, objectMapper.writeValueAsString(chunk));
                count += chunk.size();
            } catch (JsonProcessingException e) {
                log.error("压测批次序列化失败, 起始下标={}", i, e);
            }
        }
        return count;
    }
}
