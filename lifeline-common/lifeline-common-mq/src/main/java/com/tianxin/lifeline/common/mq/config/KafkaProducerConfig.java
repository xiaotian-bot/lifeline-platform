package com.tianxin.lifeline.common.mq.config;

import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.common.mq.serializer.EventEnvelopeSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 生产端高性能配置。
 *
 * <p>针对 10万条/秒 传感数据接入场景调优：
 * <ul>
 *     <li>batch.size = 262144（256KB 批次）</li>
 *     <li>linger.ms = 10（攒批等待 10ms）</li>
 *     <li>compression.type = lz4（高吞吐压缩）</li>
 * </ul>
 */
@Configuration
public class KafkaProducerConfig {

    private final Environment env;

    public KafkaProducerConfig(Environment env) {
        this.env = env;
    }

    /**
     * 构建生产端配置参数。
     *
     * @return 生产端配置 Map
     */
    private Map<String, Object> producerConfigs() {
        String servers = env.getProperty("SPRING_KAFKA_BOOTSTRAP_SERVERS",
                env.getProperty("spring.kafka.bootstrap-servers", "kafka:9094"));
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, EventEnvelopeSerializer.class);
        // 高性能攒批参数
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 262144);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024L);
        // acks=all + 重试，保证数据可靠
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return props;
    }

    /**
     * 事件信封生产者工厂。
     *
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, EventEnvelope<?>> lifelineProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * 平台统一 KafkaTemplate（事件信封类型）。
     *
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, EventEnvelope<?>> lifelineKafkaTemplate() {
        return new KafkaTemplate<>(lifelineProducerFactory());
    }
}
