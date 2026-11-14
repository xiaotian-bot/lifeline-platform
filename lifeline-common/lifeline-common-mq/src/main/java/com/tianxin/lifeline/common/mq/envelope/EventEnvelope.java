package com.tianxin.lifeline.common.mq.envelope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一事件信封。
 *
 * <p>全平台所有 Kafka 事件统一使用该信封包装，保证事件可追溯、可路由：
 * <pre>
 * {
 *   "eventId": "全局唯一事件ID",
 *   "eventType": "事件类型，如 SENSOR_RAW / WARNING_CREATED",
 *   "source": "事件来源服务或组件",
 *   "timestamp": 事件产生毫秒时间戳,
 *   "payload": { 业务数据 }
 * }
 * </pre>
 *
 * @param <T> 业务负载类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件唯一ID（建议使用雪花ID或UUID） */
    private String eventId;

    /** 事件类型编码 */
    private String eventType;

    /** 事件来源（服务名/组件名） */
    private String source;

    /** 事件产生时间戳（毫秒） */
    private Long timestamp;

    /** 业务负载 */
    private T payload;

    /**
     * 快速构建事件信封（自动生成事件ID与时间戳）。
     *
     * @param eventType 事件类型
     * @param source    事件来源
     * @param payload   业务负载
     * @param <T>       负载类型
     * @return 事件信封
     */
    public static <T> EventEnvelope<T> of(String eventType, String source, T payload) {
        EventEnvelope<T> envelope = new EventEnvelope<>();
        envelope.setEventId(cn.hutool.core.util.IdUtil.fastSimpleUUID());
        envelope.setEventType(eventType);
        envelope.setSource(source);
        envelope.setTimestamp(System.currentTimeMillis());
        envelope.setPayload(payload);
        return envelope;
    }

    /**
     * 将通用 payload 转换为具体业务类型（供消费端使用）。
     *
     * @param targetType 目标类型
     * @param <R>        目标泛型
     * @return 转换后的业务对象，payload 为 null 返回 null
     */
    public <R> R convertPayload(Class<R> targetType) {
        if (payload == null) {
            return null;
        }
        if (targetType.isInstance(payload)) {
            return targetType.cast(payload);
        }
        return PAYLOAD_MAPPER.convertValue(payload, targetType);
    }

    /** payload 类型转换专用 ObjectMapper */
    private static final com.fasterxml.jackson.databind.ObjectMapper PAYLOAD_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
}
