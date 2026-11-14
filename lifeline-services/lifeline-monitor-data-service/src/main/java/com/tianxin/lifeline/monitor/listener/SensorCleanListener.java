package com.tianxin.lifeline.monitor.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.monitor.dto.SensorRawMessage;
import com.tianxin.lifeline.monitor.service.SensorIngestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 清洗后传感数据 Kafka 监听器。
 *
 * <p><b>P6 消费源切换说明</b>：本监听器原订阅 {@link TopicConstants#PIPE_SENSOR_RAW}，
 * Flink 实时链路上线后切换为订阅 {@link TopicConstants#PIPE_SENSOR_CLEAN}
 * （Flink SensorCleanJob 的清洗输出）。清洗输出沿用与原始上报完全一致的
 * 裸 JSON 字段结构，因此解析/校验/攒批逻辑全部保留不变。
 * 单条/JSON Array 两种消息体的兼容逻辑同样保留（清洗作业输出单条格式，
 * 数组兼容作为防御性能力应对历史回放等场景）。
 *
 * <p>使用 {@code sensorCleanContainerFactory}（见 SensorCleanConsumerConfig，
 * String 键值 + 手动提交偏移量）消费。
 *
 * <p>字段校验约定：deviceId 空白或 ts 为空/非正的消息判定为非法，
 * 记录 warn 日志后跳过该条，绝不中断消费；整条消息解析失败同样仅告警，
 * 处理完成后始终提交偏移量（避免毒消息阻塞分区）。
 */
@Slf4j
@Component
public class SensorCleanListener {

    /** JSON 解析器（忽略未知字段，兼容契约演进） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** JSON Array 消息的批量类型引用 */
    private static final TypeReference<List<SensorRawMessage>> LIST_TYPE =
            new TypeReference<>() {
            };

    /** 传感数据接入服务 */
    private final SensorIngestService sensorIngestService;

    /**
     * 构造监听器。
     *
     * @param sensorIngestService 传感数据接入服务
     */
    public SensorCleanListener(SensorIngestService sensorIngestService) {
        this.sensorIngestService = sensorIngestService;
    }

    /**
     * 消费清洗后传感数据消息。
     *
     * @param payload 消息体（单条 JSON 对象或 JSON Array）
     * @param ack     手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_SENSOR_CLEAN, containerFactory = "sensorCleanContainerFactory")
    public void onSensorClean(String payload, Acknowledgment ack) {
        try {
            handleMessage(payload);
        } catch (Exception e) {
            // 兜底保护：任何未预期异常仅告警，不中断消费线程
            log.error("传感数据消息处理异常, 消息已跳过", e);
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * 解析并分发单条 Kafka 消息（兼容 JSON 对象与 JSON Array）。
     *
     * @param payload 消息体
     */
    private void handleMessage(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("收到空消息, 已跳过");
            return;
        }
        String trimmed = payload.trim();
        if (trimmed.startsWith("[")) {
            handleArray(trimmed);
            return;
        }
        SensorRawMessage message = parseSingle(trimmed);
        if (message == null) {
            return;
        }
        if (isValid(message)) {
            sensorIngestService.ingest(message);
        }
    }

    /**
     * 处理 JSON Array 消息（压测模式批量/历史回放）。
     *
     * @param arrayJson JSON Array 字符串
     */
    private void handleArray(String arrayJson) {
        List<SensorRawMessage> batch;
        try {
            batch = OBJECT_MAPPER.readValue(arrayJson, LIST_TYPE);
        } catch (Exception e) {
            log.warn("JSON Array 消息解析失败, 整条跳过: {}", abbreviate(arrayJson), e);
            return;
        }
        List<SensorRawMessage> valid = batch.stream().filter(this::isValid).toList();
        int skipped = batch.size() - valid.size();
        if (skipped > 0) {
            log.warn("JSON Array 批次中 {} 条非法数据(deviceId/ts缺失)已跳过", skipped);
        }
        if (!valid.isEmpty()) {
            sensorIngestService.ingestBatch(valid);
        }
    }

    /**
     * 解析单条 JSON 对象消息。
     *
     * @param json JSON 对象字符串
     * @return 传感数据，解析失败返回 null
     */
    private SensorRawMessage parseSingle(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, SensorRawMessage.class);
        } catch (Exception e) {
            log.warn("单条传感数据解析失败, 已跳过: {}", abbreviate(json), e);
            return null;
        }
    }

    /**
     * 字段合法性校验：deviceId 非空白且 ts 非空为正数。
     *
     * @param message 传感数据
     * @return true 表示合法
     */
    private boolean isValid(SensorRawMessage message) {
        if (message == null || message.getDeviceId() == null || message.getDeviceId().isBlank()) {
            log.warn("非法传感数据(deviceId为空), 已跳过: {}", message);
            return false;
        }
        if (message.getTs() == null || message.getTs() <= 0) {
            log.warn("非法传感数据(ts缺失或非正), 已跳过: deviceId={}", message.getDeviceId());
            return false;
        }
        return true;
    }

    /**
     * 截断过长文本用于日志输出。
     *
     * @param text 原始文本
     * @return 最长 200 字符的摘要
     */
    private String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
