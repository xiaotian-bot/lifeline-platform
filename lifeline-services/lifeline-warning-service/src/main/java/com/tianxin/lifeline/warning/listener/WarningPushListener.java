package com.tianxin.lifeline.warning.listener;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.warning.constant.WarningRedisKeys;
import com.tianxin.lifeline.warning.dto.WarningPushPayload;
import com.tianxin.lifeline.warning.service.WarningPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 预警推送 Kafka 监听器。
 *
 * <p>订阅 {@link TopicConstants#PIPE_WARNING_PUSH}（Topic 名引用常量，禁止硬编码），
 * 使用 common-mq 的 {@code lifelineKafkaListenerContainerFactory}
 * （EventEnvelope 信封 + MANUAL_IMMEDIATE 手动提交）消费推送消息，
 * 落库 warning_push_record 完成推送留痕。
 *
 * <p>以信封 eventId 做 Redis SETNX 幂等，防止重复消费产生重复推送记录。
 */
@Slf4j
@Component
public class WarningPushListener {

    /** 推送幂等键 TTL（分钟） */
    private static final long IDEMPOTENT_TTL_MINUTES = 10L;

    /** 预警推送服务 */
    private final WarningPushService warningPushService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造监听器。
     *
     * @param warningPushService  预警推送服务
     * @param stringRedisTemplate Redis 模板
     */
    public WarningPushListener(WarningPushService warningPushService,
                               StringRedisTemplate stringRedisTemplate) {
        this.warningPushService = warningPushService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 消费推送消息并落库留痕。
     *
     * @param envelope 事件信封
     * @param ack      手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_WARNING_PUSH,
            containerFactory = "lifelineKafkaListenerContainerFactory")
    public void onWarningPush(EventEnvelope<Object> envelope, Acknowledgment ack) {
        try {
            handle(envelope);
        } catch (Exception e) {
            // 兜底保护：任何未预期异常仅告警，不中断消费线程
            log.error("预警推送消费处理异常, 消息已跳过", e);
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * 处理单个推送信封：幂等去重后落库。
     *
     * @param envelope 事件信封
     */
    private void handle(EventEnvelope<Object> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("收到空预警推送信封, 已跳过");
            return;
        }
        String idemKey = WarningRedisKeys.pushIdempotent(envelope.getEventId());
        Boolean firstSeen = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", Duration.ofMinutes(IDEMPOTENT_TTL_MINUTES));
        if (!Boolean.TRUE.equals(firstSeen)) {
            log.debug("预警推送重复消费已拦截: eventId={}", envelope.getEventId());
            return;
        }
        WarningPushPayload payload = envelope.convertPayload(WarningPushPayload.class);
        if (payload == null) {
            log.warn("预警推送负载转换失败, 已跳过: eventId={}", envelope.getEventId());
            return;
        }
        warningPushService.savePushRecord(payload);
    }
}
