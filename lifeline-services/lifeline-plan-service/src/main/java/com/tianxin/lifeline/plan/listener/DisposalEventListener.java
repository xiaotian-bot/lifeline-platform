package com.tianxin.lifeline.plan.listener;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.plan.config.PlanProperties;
import com.tianxin.lifeline.plan.constant.PlanRedisKeys;
import com.tianxin.lifeline.plan.dto.DisposalFeedbackPayload;
import com.tianxin.lifeline.plan.service.PlanExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 处置反馈事件 Kafka 监听器。
 *
 * <p>订阅 {@link TopicConstants#PIPE_DISPOSAL_EVENT}（Topic 名引用常量，禁止硬编码），
 * 使用 common-mq 的 {@code lifelineKafkaListenerContainerFactory}
 * （EventEnvelope 信封 + MANUAL_IMMEDIATE 手动提交）消费工单服务在工单完成时
 * 发布的处置反馈事件，按 warningEventId 关联回写预案任务状态并核销实例。
 *
 * <p>幂等保障：Redis SETNX（eventId 级，TTL 可配）拦截重复投递，
 * 参照 warning-service 既有幂等模式。
 *
 * <p>提交语义：仅处理成功后 ack；处理失败先删除幂等键再将异常上抛，
 * 由 MANUAL_IMMEDIATE 容器重试重投，避免失败消息被静默吞掉后永久丢失；
 * 重投超限进死信后由人工介入处理。
 */
@Slf4j
@Component
public class DisposalEventListener {

    /** 预案执行业务服务 */
    private final PlanExecutionService executionService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 预案业务配置 */
    private final PlanProperties planProperties;

    /**
     * 构造监听器（构造器注入）。
     *
     * @param executionService    预案执行业务服务
     * @param stringRedisTemplate Redis 模板
     * @param planProperties      预案业务配置
     */
    public DisposalEventListener(PlanExecutionService executionService,
                                 StringRedisTemplate stringRedisTemplate,
                                 PlanProperties planProperties) {
        this.executionService = executionService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.planProperties = planProperties;
    }

    /**
     * 消费处置反馈信封并回写预案任务状态。
     *
     * <p>提交策略：处理成功后才 ack；异常时先删除幂等键再上抛，
     * 保持 MANUAL_IMMEDIATE 容器的重试语义，失败消息不静默丢失。
     *
     * @param envelope 事件信封
     * @param ack      手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_DISPOSAL_EVENT,
            containerFactory = "lifelineKafkaListenerContainerFactory")
    public void onDisposalEvent(EventEnvelope<Object> envelope, Acknowledgment ack) {
        String idemKey = null;
        try {
            idemKey = handle(envelope);
            // 仅处理成功才提交偏移量
            ack.acknowledge();
        } catch (Exception e) {
            // 处理失败：先删除幂等键为重投留出空间，再上抛异常触发容器重试
            if (idemKey != null) {
                try {
                    stringRedisTemplate.delete(idemKey);
                } catch (Exception deleteEx) {
                    log.warn("处置反馈幂等键删除失败, 重试窗口内需等待 TTL 自然过期: key={}", idemKey, deleteEx);
                }
            }
            log.error("处置反馈事件消费处理失败, 待容器重试", e);
            throw e;
        }
    }

    /**
     * 处理单个处置反馈信封：幂等去重 → 负载转换 → 关联回写。
     *
     * @param envelope 事件信封
     * @return 已占用的幂等键（供失败时回滚删除）；空信封/无 eventId 等无需幂等占位时返回 null
     */
    private String handle(EventEnvelope<Object> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("收到空处置反馈信封, 已跳过");
            return null;
        }
        if (!StringUtils.hasText(envelope.getEventId())) {
            // eventId 缺失时幂等键会退化为固定键误杀后续事件，告警并跳过
            log.warn("处置反馈信封缺失 eventId, 无法构建幂等键, 已跳过");
            return null;
        }
        // 幂等：Kafka eventId 级 Redis SETNX 去重
        String idemKey = PlanRedisKeys.disposalIdempotent(envelope.getEventId());
        Duration ttl = Duration.ofMinutes(planProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            log.debug("处置反馈事件重复投递已拦截: eventId={}", envelope.getEventId());
            return null;
        }

        DisposalFeedbackPayload payload = envelope.convertPayload(DisposalFeedbackPayload.class);
        if (payload == null) {
            log.warn("处置反馈负载转换失败, 已跳过: eventId={}", envelope.getEventId());
            return idemKey;
        }
        if (payload.getWarningEventId() == null) {
            // 无预警关联的处置反馈与预案无关，直接跳过
            return idemKey;
        }
        executionService.applyDisposalFeedback(payload);
        return idemKey;
    }
}
