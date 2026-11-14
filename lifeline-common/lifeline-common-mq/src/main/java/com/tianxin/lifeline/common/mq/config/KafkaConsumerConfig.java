package com.tianxin.lifeline.common.mq.config;

import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.common.mq.serializer.EventEnvelopeDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 消费端配置模板。
 *
 * <p>提供事件信封反序列化的消费者工厂与监听容器工厂，
 * 各服务使用 {@code @KafkaListener} 即可消费 {@link EventEnvelope}。
 */
@Configuration
public class KafkaConsumerConfig {

    private final Environment env;

    public KafkaConsumerConfig(Environment env) {
        this.env = env;
    }

    /** 消费者组ID（各服务在自己的配置文件中覆盖） */
    @Value("${spring.kafka.consumer.group-id:lifeline-default-group}")
    private String groupId;

    /**
     * 构建消费端配置参数。
     *
     * @return 消费端配置 Map
     */
    private Map<String, Object> consumerConfigs() {
        String servers = env.getProperty("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9094");
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventEnvelopeDeserializer.class);
        // 从最早位点开始消费，单次拉取最多500条
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    /**
     * 事件信封消费者工厂。
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, EventEnvelope<Object>> lifelineConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * 事件信封监听容器工厂（手动提交偏移量，3个并发消费线程）。
     *
     * @return ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<Object>> lifelineKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<Object>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(lifelineConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
