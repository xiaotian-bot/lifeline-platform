package com.tianxin.lifeline.warning.listener;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.warning.config.WarningProperties;
import com.tianxin.lifeline.warning.constant.WarningRedisKeys;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import com.tianxin.lifeline.warning.service.AiJudgeAdapter;
import com.tianxin.lifeline.warning.service.AiJudgeBackfillService;
import com.tianxin.lifeline.warning.service.WarningEventService;
import com.tianxin.lifeline.warning.service.WarningPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * 预警事件 Kafka 监听器。
 *
 * <p>订阅 {@link TopicConstants#PIPE_WARNING_EVENT}（Topic 名引用常量，禁止硬编码），
 * 使用 common-mq 的 {@code lifelineKafkaListenerContainerFactory}
 * （EventEnvelope 信封 + MANUAL_IMMEDIATE 手动提交）消费规则判定桥/
 * AI 模型产出的预警事件，完成「幂等去重 → AI 深研评分 → 落库 → 分级推送」。
 *
 * <p>幂等双重保障：Redis SETNX（eventId 级，TTL 10 分钟）先拦重复投递，
 * 数据库部分唯一索引 uk_warning_event_code 兜底并发竞争。
 */
@Slf4j
@Component
public class WarningEventListener {

    /** 预警事件业务服务 */
    private final WarningEventService warningEventService;

    /** AI 深研适配器（失败自动降级纯规则） */
    private final AiJudgeAdapter aiJudgeAdapter;

    /** AI 研判异步回填服务（落库后异步深度研判，不阻塞主链路） */
    private final AiJudgeBackfillService aiJudgeBackfillService;

    /** 预警推送服务 */
    private final WarningPushService warningPushService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 预警业务配置 */
    private final WarningProperties warningProperties;

    /**
     * 构造监听器。
     *
     * @param warningEventService    预警事件业务服务
     * @param aiJudgeAdapter         AI 深研适配器
     * @param aiJudgeBackfillService AI 研判异步回填服务
     * @param warningPushService     预警推送服务
     * @param stringRedisTemplate    Redis 模板
     * @param warningProperties      预警业务配置
     */
    public WarningEventListener(WarningEventService warningEventService,
                                AiJudgeAdapter aiJudgeAdapter,
                                AiJudgeBackfillService aiJudgeBackfillService,
                                WarningPushService warningPushService,
                                StringRedisTemplate stringRedisTemplate,
                                WarningProperties warningProperties) {
        this.warningEventService = warningEventService;
        this.aiJudgeAdapter = aiJudgeAdapter;
        this.aiJudgeBackfillService = aiJudgeBackfillService;
        this.warningPushService = warningPushService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.warningProperties = warningProperties;
    }

    /**
     * 消费预警事件信封并落库。
     *
     * @param envelope 事件信封
     * @param ack      手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_WARNING_EVENT,
            containerFactory = "lifelineKafkaListenerContainerFactory")
    public void onWarningEvent(EventEnvelope<Object> envelope, Acknowledgment ack) {
        try {
            handle(envelope);
        } catch (Exception e) {
            // 兜底保护：任何未预期异常仅告警，不中断消费线程
            log.error("预警事件消费处理异常, 消息已跳过", e);
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * 处理单个预警事件信封：幂等 → AI 评分 → 落库 → 异步研判回填 → 推送。
     *
     * @param envelope 事件信封
     */
    private void handle(EventEnvelope<Object> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("收到空预警事件信封, 已跳过");
            return;
        }
        // 第一重幂等：Kafka eventId 级 Redis SETNX 去重
        String idemKey = WarningRedisKeys.eventIdempotent(envelope.getEventId());
        Duration ttl = Duration.ofMinutes(warningProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            log.debug("预警事件重复投递已拦截: eventId={}", envelope.getEventId());
            return;
        }

        WarningEventPayload payload = envelope.convertPayload(WarningEventPayload.class);
        if (payload == null) {
            log.warn("预警事件负载转换失败, 已跳过: eventId={}", envelope.getEventId());
            return;
        }

        // AI 深研评分（失败自动降级为纯规则，返回 null）
        BigDecimal aiScore = aiJudgeAdapter.judge(payload);

        // 落库（内部做 warningNo 级幂等与唯一索引兜底）
        WarningEvent saved = warningEventService.createFromPayload(payload, aiScore);
        if (saved == null) {
            return;
        }
        // 异步 AI 深度研判回填（不阻塞主消费链路，失败降级不回填）
        aiJudgeBackfillService.submitAsyncJudge(saved);
        // 按等级匹配推送对象，经 pipe_warning_push 异步落库留痕
        warningPushService.publishPushes(saved);
    }
}
