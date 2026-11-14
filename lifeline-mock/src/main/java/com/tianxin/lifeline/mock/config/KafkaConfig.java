package com.tianxin.lifeline.mock.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock 系统 Kafka 生产者配置。
 *
 * <p>原始传感数据以 JSON 字符串形式上报（压测模式下为 JSON Array），
 * 因此值序列化器使用 {@link StringSerializer}；与公共模块
 * {@code KafkaProducerConfig} 的 EventEnvelope 生产者并存、互不干扰。
 *
 * <p>沿用高吞吐攒批参数：256KB 批次、10ms 攒批等待、lz4 压缩。
 */
@Configuration
public class KafkaConfig {

    /** Kafka 集群地址 */
    @Value("${spring.kafka.bootstrap-servers:kafka:9094}")
    private String bootstrapServers;

    /**
     * 构建原始数据生产端配置参数。
     *
     * @return 生产端配置 Map
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 高性能攒批参数（与平台统一调优标准一致）
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 262144);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024L);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    /**
     * 原始传感数据生产者工厂。
     *
     * @return ProducerFactory（String 键 / String 值）
     */
    @Bean
    public ProducerFactory<String, String> mockRawProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * 原始传感数据专用 KafkaTemplate。
     *
     * @return KafkaTemplate（String 键 / String 值）
     */
    @Bean
    public KafkaTemplate<String, String> rawKafkaTemplate() {
        return new KafkaTemplate<>(mockRawProducerFactory());
    }
}
