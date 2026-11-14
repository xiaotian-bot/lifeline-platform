package com.tianxin.lifeline.flink.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson ObjectMapper 统一工厂。
 *
 * <p>两类口径：
 * <ul>
 *     <li>{@link #sensorMapper()}：传感数据读写，忽略未知字段（兼容契约演进），
 *         序列化时忽略 null 字段（与 lifeline-mock 输出风格一致）；</li>
 *     <li>{@link #eventMapper()}：预警事件信封序列化，注册 JavaTimeModule 且
 *         <b>不</b>关闭 WRITE_DATES_AS_TIMESTAMPS —— 与 common-mq 的
 *         EventEnvelopeSerializer 完全同口径（LocalDateTime 序列化为数组形式），
 *         保证 warning-service 的 EventEnvelopeDeserializer 可原样反序列化。</li>
 * </ul>
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /** 传感数据专用 ObjectMapper */
    private static final ObjectMapper SENSOR_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** 事件信封专用 ObjectMapper（对齐 common-mq EventEnvelopeSerializer 口径） */
    private static final ObjectMapper EVENT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 传感数据读写 ObjectMapper。
     *
     * @return 共享实例（线程安全）
     */
    public static ObjectMapper sensorMapper() {
        return SENSOR_MAPPER;
    }

    /**
     * 事件信封序列化 ObjectMapper。
     *
     * @return 共享实例（线程安全）
     */
    public static ObjectMapper eventMapper() {
        return EVENT_MAPPER;
    }
}
