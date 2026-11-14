package com.tianxin.lifeline.common.mq.template;

import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 事件发送模板。
 *
 * <p>封装 {@link KafkaTemplate}，统一事件信封包装与发送日志，
 * 业务侧一行代码即可发送标准事件：
 * <pre>
 * kafkaEventTemplate.sendEvent(TopicConstants.PIPE_WARNING_EVENT, "WARNING_CREATED", payload);
 * </pre>
 */
@Slf4j
@Component
public class KafkaEventTemplate {

    /** 平台统一 KafkaTemplate */
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    /**
     * 构造发送模板。
     *
     * @param kafkaTemplate 平台统一 KafkaTemplate
     */
    public KafkaEventTemplate(KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送事件（自动包装为事件信封，自动生成事件ID与时间戳）。
     *
     * @param topic     目标 Topic
     * @param eventType 事件类型
     * @param source    事件来源（服务名）
     * @param payload   业务负载
     * @param <T>       负载类型
     * @return 发送结果的 CompletableFuture
     */
    public <T> CompletableFuture<SendResult<String, EventEnvelope<?>>> sendEvent(
            String topic, String eventType, String source, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, source, payload);
        return sendEnvelope(topic, null, envelope);
    }

    /**
     * 发送事件（指定分区键）。
     *
     * @param topic     目标 Topic
     * @param key       分区键（如设备ID，保证同设备数据有序）
     * @param eventType 事件类型
     * @param source    事件来源（服务名）
     * @param payload   业务负载
     * @param <T>       负载类型
     * @return 发送结果的 CompletableFuture
     */
    public <T> CompletableFuture<SendResult<String, EventEnvelope<?>>> sendEvent(
            String topic, String key, String eventType, String source, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(eventType, source, payload);
        return sendEnvelope(topic, key, envelope);
    }

    /**
     * 直接发送完整事件信封。
     *
     * @param topic    目标 Topic
     * @param key      分区键，可为 null
     * @param envelope 事件信封
     * @param <T>      负载类型
     * @return 发送结果的 CompletableFuture
     */
    public <T> CompletableFuture<SendResult<String, EventEnvelope<?>>> sendEnvelope(
            String topic, String key, EventEnvelope<T> envelope) {
        log.debug("发送Kafka事件: topic={}, key={}, eventId={}, eventType={}",
                topic, key, envelope.getEventId(), envelope.getEventType());
        return kafkaTemplate.send(topic, key, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka事件发送失败: topic={}, eventId={}", topic, envelope.getEventId(), ex);
                    }
                });
    }
}
