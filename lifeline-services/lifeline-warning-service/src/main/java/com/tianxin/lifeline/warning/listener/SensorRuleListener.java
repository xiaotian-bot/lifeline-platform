package com.tianxin.lifeline.warning.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.warning.dto.SensorRawMessage;
import com.tianxin.lifeline.warning.service.RuleJudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

/**
 * 原始传感数据 Kafka 监听器（规则判定桥入口）。
 *
 * <p>订阅 {@link TopicConstants#PIPE_SENSOR_RAW}（Topic 名引用常量，禁止硬编码），
 * 使用 {@code sensorRawContainerFactory}（见 SensorRawConsumerConfig，String 键值 +
 * 手动提交偏移量）消费 lifeline-mock 上报的裸 JSON 消息，兼容两种消息体格式：
 * <ul>
 *     <li>常规模式：单条 JSON 对象；</li>
 *     <li>压测模式：100 条数据合并的 JSON Array。</li>
 * </ul>
 *
 * <p>每条合法数据交给 {@link RuleJudgeService} 做规则越限判定；
 * 字段校验约定：deviceId 空白或 ts 缺失/非正的消息判定为非法，
 * 记录 warn 日志后跳过该条，绝不中断消费；处理完成后始终提交偏移量
 * （避免毒消息阻塞分区）。
 *
 * <p><b>Flink 切流开关</b>：当 Flink RuleWarningJob 上线接管规则判定后，
 * 将 {@code lifeline.warning.rule-bridge.enabled} 设为 {@code false} 即可
 * 禁用本监听器（与 {@link com.tianxin.lifeline.warning.config.SensorRawConsumerConfig}
 * 同步失效），无需改代码。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "lifeline.warning.rule-bridge.enabled", havingValue = "true", matchIfMissing = true)
public class SensorRuleListener {

    /** JSON 解析器（忽略未知字段，兼容契约演进） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** JSON Array 消息的批量类型引用 */
    private static final TypeReference<List<SensorRawMessage>> LIST_TYPE =
            new TypeReference<>() {
            };

    /** 规则判定服务 */
    private final RuleJudgeService ruleJudgeService;

    /**
     * 构造监听器。
     *
     * @param ruleJudgeService 规则判定服务
     */
    public SensorRuleListener(RuleJudgeService ruleJudgeService) {
        this.ruleJudgeService = ruleJudgeService;
    }

    /**
     * 消费原始传感数据消息并做规则判定。
     *
     * @param payload 消息体（单条 JSON 对象或 JSON Array）
     * @param ack     手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_SENSOR_RAW, containerFactory = "sensorRawContainerFactory")
    public void onSensorRaw(String payload, Acknowledgment ack) {
        try {
            handleMessage(payload);
        } catch (Exception e) {
            // 兜底保护：任何未预期异常仅告警，不中断消费线程
            log.error("传感数据规则判定异常, 消息已跳过", e);
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
        if (message != null && isValid(message)) {
            ruleJudgeService.judge(message);
        }
    }

    /**
     * 处理 JSON Array 消息（压测模式批量）。
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
        for (SensorRawMessage message : batch) {
            if (isValid(message)) {
                try {
                    ruleJudgeService.judge(message);
                } catch (Exception e) {
                    log.error("单条数据规则判定异常, 已跳过: deviceId={}", message.getDeviceId(), e);
                }
            }
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
            return false;
        }
        return message.getTs() != null && message.getTs() > 0;
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
