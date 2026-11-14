package com.tianxin.lifeline.flink.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tianxin.lifeline.flink.model.SensorMessage;

import java.util.Collections;
import java.util.List;

/**
 * 传感数据裸 JSON 解析器（兼容两种消息体契约）。
 *
 * <p>lifeline-mock 的 KafkaDataSender 上报两种格式：
 * <ul>
 *     <li>常规模式：单条 JSON 对象；</li>
 *     <li>压测模式：100 条合并的 JSON Array。</li>
 * </ul>
 * 解析策略：先尝试按 JSON Array 解析，失败后回退按单条 JSON 对象解析，
 * 与 monitor/warning 服务现有监听器的兼容逻辑保持一致；
 * 字段校验约定：deviceId 空白或 ts 为空/非正判定为非法数据。
 */
public final class SensorMessageParser {

    /** JSON Array 批量类型引用 */
    private static final TypeReference<List<SensorMessage>> LIST_TYPE =
            new TypeReference<>() {
            };

    private SensorMessageParser() {
    }

    /**
     * 解析单条 Kafka 消息（可能包含多条数据）。
     *
     * @param payload 消息体（单条 JSON 对象或 JSON Array），null/空白返回空列表
     * @return 解析出的数据列表（不做合法性过滤），整条解析失败返回空列表
     */
    public static List<SensorMessage> parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = payload.trim();
        // 优先按 JSON Array 解析（压测模式）
        if (trimmed.startsWith("[")) {
            try {
                List<SensorMessage> batch = JsonUtils.sensorMapper().readValue(trimmed, LIST_TYPE);
                return batch == null ? Collections.emptyList() : batch;
            } catch (Exception ignored) {
                // 数组解析失败继续回退单条解析
            }
        }
        // 回退按单条 JSON 对象解析
        try {
            SensorMessage message = JsonUtils.sensorMapper().readValue(trimmed, SensorMessage.class);
            return message == null ? Collections.emptyList() : List.of(message);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 字段合法性校验：deviceId 非空白且 ts 非空为正数。
     *
     * @param message 传感数据
     * @return true 表示合法
     */
    public static boolean isValid(SensorMessage message) {
        if (message == null || message.getDeviceId() == null || message.getDeviceId().isBlank()) {
            return false;
        }
        return message.getTs() != null && message.getTs() > 0;
    }
}
