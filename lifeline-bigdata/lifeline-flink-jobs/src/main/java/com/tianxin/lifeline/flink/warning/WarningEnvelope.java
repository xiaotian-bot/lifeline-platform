package com.tianxin.lifeline.flink.warning;

import cn.hutool.core.util.IdUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一事件信封（Flink 侧副本）。
 *
 * <p>字段名与 common-mq 的 EventEnvelope <b>完全一致</b>
 * （eventId / eventType / source / timestamp / payload），
 * JSON 结构对齐后 warning-service 的 EventEnvelopeDeserializer 可直接消费。
 * eventId 采用与 {@code EventEnvelope.of} 相同的 hutool fastSimpleUUID 生成方式。
 *
 * @param <T> 业务负载类型
 */
@Data
public class WarningEnvelope<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件唯一ID（UUID，下游幂等键） */
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
     * 快速构建事件信封（自动生成事件ID与时间戳，与 EventEnvelope.of 同语义）。
     *
     * @param eventType 事件类型
     * @param source    事件来源
     * @param payload   业务负载
     * @param <T>       负载类型
     * @return 事件信封
     */
    public static <T> WarningEnvelope<T> of(String eventType, String source, T payload) {
        WarningEnvelope<T> envelope = new WarningEnvelope<>();
        envelope.setEventId(IdUtil.fastSimpleUUID());
        envelope.setEventType(eventType);
        envelope.setSource(source);
        envelope.setTimestamp(System.currentTimeMillis());
        envelope.setPayload(payload);
        return envelope;
    }
}
