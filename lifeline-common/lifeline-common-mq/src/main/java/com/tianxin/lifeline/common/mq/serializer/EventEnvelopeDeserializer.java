package com.tianxin.lifeline.common.mq.serializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * 事件信封 Kafka 反序列化器。
 *
 * <p>将 Kafka 中的 JSON 字节数组反序列化为 {@link EventEnvelope}，
 * payload 默认解析为通用结构（Map/List），消费端可使用
 * {@link EventEnvelope#convertPayload(Class)} 转换为具体业务类型。
 */
@Slf4j
public class EventEnvelopeDeserializer implements Deserializer<EventEnvelope<Object>> {

    /** 共享 ObjectMapper（忽略未知字段，兼容事件结构演进） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 反序列化事件信封。
     *
     * @param topic 来源 Topic
     * @param data  JSON 字节数组
     * @return 事件信封，data 为 null 返回 null
     */
    @Override
    public EventEnvelope<Object> deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(data,
                    OBJECT_MAPPER.getTypeFactory().constructParametricType(EventEnvelope.class, Object.class));
        } catch (Exception e) {
            log.error("事件信封反序列化失败, topic={}", topic, e);
            return null;
        }
    }
}
