package com.tianxin.lifeline.operation.config;

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
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * 预警自动派单专用 Kafka 消费端配置。
 *
 * <p>common-mq 的 {@code lifelineKafkaListenerContainerFactory} 未配置
 * 错误处理器，spring-kafka 默认 {@code FixedBackOff(0,9)} 快速重试后
 * 直接丢弃记录，建单失败将永久丢失橙/红预警工单。故在 operation-service
 * 内新建专用监听容器工厂（关键配置与 common-mq 工厂保持一致：
 * EventEnvelope 信封反序列化、MANUAL_IMMEDIATE 手动提交、group 注入方式），
 * 并配置 {@link DefaultErrorHandler} + {@code FixedBackOff(2000, 10)}：
 * 每 2 秒重试一次、最多 10 次；重试耗尽后记录 error 日志告警并放弃该记录
 * （不引入死信主题）。
 *
 * <p>不修改 common-mq 公共模块，其他监听器仍使用公共工厂不受影响。
 */
@Configuration
public class AutoDispatchConsumerConfig {

    /** Kafka 集群地址 */
    @Value("${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9094}")
    private String bootstrapServers;

    /** 消费者组ID（与公共工厂一致的注入方式，本服务配置为 lifeline-operation-group） */
    @Value("${spring.kafka.consumer.group-id:lifeline-default-group}")
    private String groupId;

    /** 重试间隔（毫秒） */
    private static final long RETRY_INTERVAL_MS = 2000L;

    /** 最大重试次数（重试耗尽后记录 error 告警并放弃） */
    private static final long MAX_RETRY_ATTEMPTS = 10L;

    /**
     * 构建消费端配置参数（与 common-mq 公共工厂关键配置一致）。
     *
     * @return 消费端配置 Map
     */
    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
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
     * 自动派单专用事件信封消费者工厂。
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, EventEnvelope<Object>> autoDispatchConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    /**
     * 自动派单专用错误处理器：2 秒间隔重试最多 10 次，
     * 重试耗尽后记录 error 日志告警并放弃该记录（不引入死信主题）。
     *
     * @return CommonErrorHandler
     */
    private CommonErrorHandler autoDispatchErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> logExhausted(record, exception),
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS));
        return errorHandler;
    }

    /**
     * 重试耗尽回调：记录 error 日志告警（橙/红预警工单丢失风险人工介入）。
     *
     * @param record    消费失败的记录
     * @param exception 最后一次异常
     */
    private void logExhausted(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception exception) {
        org.slf4j.LoggerFactory.getLogger(AutoDispatchConsumerConfig.class).error(
                "预警自动派单消费重试{}次后仍失败, 记录已放弃(topic={}, partition={}, offset={}), "
                        + "请人工核查该预警是否已生成工单",
                MAX_RETRY_ATTEMPTS, record.topic(), record.partition(), record.offset(), exception);
    }

    /**
     * 自动派单专用监听容器工厂（MANUAL_IMMEDIATE 手动提交，3个并发消费线程，
     * 附重试错误处理器）。
     *
     * @return ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<Object>>
            warningAutoDispatchContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<Object>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(autoDispatchConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(autoDispatchErrorHandler());
        return factory;
    }
}
