package com.tianxin.lifeline.blockchain.listener;

import com.tianxin.lifeline.blockchain.config.BlockchainProperties;
import com.tianxin.lifeline.blockchain.constant.BlockchainRedisKeys;
import com.tianxin.lifeline.blockchain.dto.EvidenceRequestPayload;
import com.tianxin.lifeline.blockchain.service.EvidenceService;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 存证请求 Kafka 监听器。
 *
 * <p>订阅 {@link TopicConstants#PIPE_EVIDENCE_REQUEST}（Topic 名引用常量，禁止硬编码），
 * 使用 common-mq 的 {@code lifelineKafkaListenerContainerFactory}
 * （EventEnvelope 信封 + MANUAL_IMMEDIATE 手动提交）消费 operation / construction / plan
 * 三服务按 {@link EvidenceRequestPayload} 契约生产的存证请求，自动完成存证上链。
 *
 * <p>幂等保障：信封 eventId 级 Redis SETNX 去重（TTL 可配），重复投递自动拦截。
 */
@Slf4j
@Component
public class EvidenceRequestListener {

    /** 存证业务服务 */
    private final EvidenceService evidenceService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 区块链业务配置 */
    private final BlockchainProperties blockchainProperties;

    /**
     * 构造监听器。
     *
     * @param evidenceService        存证业务服务
     * @param stringRedisTemplate    Redis 模板
     * @param blockchainProperties   区块链业务配置
     */
    public EvidenceRequestListener(EvidenceService evidenceService,
                                   StringRedisTemplate stringRedisTemplate,
                                   BlockchainProperties blockchainProperties) {
        this.evidenceService = evidenceService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.blockchainProperties = blockchainProperties;
    }

    /**
     * 消费存证请求信封并自动存证上链。
     *
     * @param envelope 事件信封
     * @param ack      手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_EVIDENCE_REQUEST,
            containerFactory = "lifelineKafkaListenerContainerFactory")
    public void onEvidenceRequest(EventEnvelope<Object> envelope, Acknowledgment ack) {
        try {
            handle(envelope);
        } catch (Exception e) {
            // 兜底保护：任何未预期异常仅告警，不中断消费线程
            log.error("存证请求消费处理异常, 消息已跳过", e);
        } finally {
            ack.acknowledge();
        }
    }

    /**
     * 处理单个存证请求信封：幂等去重 → 负载转换 → 自动存证上链。
     *
     * @param envelope 事件信封
     */
    private void handle(EventEnvelope<Object> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("收到空存证请求信封, 已跳过");
            return;
        }

        // 幂等：信封 eventId 级 Redis SETNX 去重
        String idemKey = BlockchainRedisKeys.evidenceIdempotent(envelope.getEventId());
        Duration ttl = Duration.ofMinutes(blockchainProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            log.debug("存证请求重复投递已拦截: eventId={}", envelope.getEventId());
            return;
        }

        EvidenceRequestPayload payload = envelope.convertPayload(EvidenceRequestPayload.class);
        if (payload == null) {
            log.warn("存证请求负载转换失败, 已跳过: eventId={}", envelope.getEventId());
            return;
        }

        evidenceService.depositFromPayload(payload);
        log.info("存证请求自动上链完成: eventId={}, type={}, source={}",
                envelope.getEventId(), payload.getEvidenceType(), envelope.getSource());
    }
}
