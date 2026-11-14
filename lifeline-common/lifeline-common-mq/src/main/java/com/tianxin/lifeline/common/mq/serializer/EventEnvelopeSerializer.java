package com.tianxin.lifeline.common.mq.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

/**
 * 事件信封 Kafka 序列化器。
 *
 * <p>将 {@link EventEnvelope} 序列化为 UTF-8 JSON 字节数组写入 Kafka。
 */
@Slf4j
public class EventEnvelopeSerializer implements Serializer<EventEnvelope<?>> {

    /** 共享 ObjectMapper（注册 JavaTime 模块支持 LocalDateTime 等类型） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 序列化事件信封。
     *
     * @param topic 目标 Topic
     * @param data  事件信封
     * @return JSON 字节数组，data 为 null 返回 null
     */
    @Override
    public byte[] serialize(String topic, EventEnvelope<?> data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            log.error("事件信封序列化失败, topic={}, eventId={}", topic, data.getEventId(), e);
            throw new IllegalStateException("EventEnvelope 序列化失败", e);
        }
    }
}
