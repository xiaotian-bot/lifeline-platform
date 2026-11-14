package com.tianxin.lifeline.monitor.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 原始传感数据 Kafka 消费端配置。
 *
 * <p><b>为什么不直接复用 common-mq 的 {@code KafkaConsumerConfig}（EventEnvelope 体系）</b>：
 * lifeline-mock 的 {@code KafkaDataSender} 以 String 序列化器直接向
 * {@code pipe_sensor_raw} 发送裸 SensorData JSON（常规模式为单条 JSON 对象，
 * 压测模式为 100 条 JSON Array），并非 EventEnvelope 信封结构。若使用
 * {@code EventEnvelopeDeserializer}：单条 JSON 会被解析为 payload=null 的空信封，
 * JSON Array 则直接反序列化失败返回 null，两种格式的数据均会丢失。
 *
 * <p>因此本服务仿照 {@code KafkaConsumerConfig} 的体系结构，为
 * {@code pipe_sensor_raw} 单独构建 String 键值的监听容器工厂
 * （Bean 名 {@code sensorRawContainerFactory}），消费参数读取
 * {@code spring.kafka.consumer.*} 配置；偏移量采用手动提交，
 * 保证「先处理、后提交」的至少一次语义。
 */
@Configuration
public class SensorRawConsumerConfig {

    /** Kafka 集群地址 */
    @Value("${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9094}")
    private String bootstrapServers;

    /** 消费者组ID */
    @Value("${spring.kafka.consumer.group-id:monitor-data-group}")
    private String groupId;

    /** 无位点时的起始消费策略 */
    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    /** 单次拉取最大条数 */
    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private int maxPollRecords;

    /**
     * 构建原始传感数据消费端配置参数。
     *
     * @return 消费端配置 Map
     */
    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        // 手动提交偏移量，由监听器在处理完成后显式 ack
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    /**
     * 原始传感数据消费者工厂（String 键值）。
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, String> sensorRawConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * 原始传感数据监听容器工厂（手动提交偏移量，3个并发消费线程）。
     *
     * @return ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> sensorRawContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(sensorRawConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
