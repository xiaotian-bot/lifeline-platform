package com.tianxin.lifeline.construction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.template.KafkaEventTemplate;
import com.tianxin.lifeline.construction.dto.EvidenceRequestPayload;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 违规存证请求事件生产者。
 *
 * <p>补齐 blockchain-service {@code EvidenceRequestListener} 缺失的
 * 施工域上游生产者：视觉识别判定违规或人工上报违规时，按消费端
 * {@code EvidenceRequestPayload} 字段契约组装负载，经
 * {@link KafkaEventTemplate} 包装 EventEnvelope 信封投递
 * {@link TopicConstants#PIPE_EVIDENCE_REQUEST}（Topic 引用常量，禁止硬编码），
 * 由区块链服务消费完成存证上链。
 *
 * <p>信封 eventId 由 KafkaEventTemplate 自动生成，消费端据此
 * Redis SETNX 幂等去重；分区键取项目ID，保证同项目存证有序。
 */
@Slf4j
@Component
public class EvidenceProducer {

    /** 存证类型：施工环节 */
    public static final String EVIDENCE_TYPE_CONSTRUCTION = "CONSTRUCTION";

    /** 关联对象类型：施工项目 */
    public static final String RELATED_TYPE_PROJECT = "CONSTRUCTION_PROJECT";

    /** 事件类型 */
    public static final String EVENT_TYPE = "EVIDENCE_REQUEST";

    /** 事件来源（服务名，与 Nacos 注册名一致） */
    public static final String EVENT_SOURCE = "construction-service";

    /** Kafka 事件发送模板 */
    private final KafkaEventTemplate kafkaEventTemplate;

    /** 存证内容 JSON 序列化器 */
    private static final ObjectMapper CONTENT_MAPPER = new ObjectMapper();

    /**
     * 构造生产者。
     *
     * @param kafkaEventTemplate Kafka 事件发送模板
     */
    public EvidenceProducer(KafkaEventTemplate kafkaEventTemplate) {
        this.kafkaEventTemplate = kafkaEventTemplate;
    }

    /**
     * 投递违规存证请求事件（AI 视觉命中与人工上报共用）。
     *
     * @param violation   已落库的违规记录
     * @param projectName 项目名称（冗余进存证内容，便于链上可读）
     * @param source      违规来源：VISION AI视觉 / MANUAL 人工上报
     * @param reporter    上报人/识别来源标识
     */
    public void sendViolationEvidence(ConstructionViolation violation,
                                      String projectName,
                                      String source,
                                      String reporter) {
        EvidenceRequestPayload payload = new EvidenceRequestPayload();
        payload.setEvidenceType(EVIDENCE_TYPE_CONSTRUCTION);
        payload.setRelatedId(violation.getProjectId());
        payload.setRelatedType(RELATED_TYPE_PROJECT);
        payload.setContent(buildContent(violation, projectName, source, reporter));
        payload.setPipeCode(null);

        kafkaEventTemplate.sendEvent(TopicConstants.PIPE_EVIDENCE_REQUEST,
                        String.valueOf(violation.getProjectId()),
                        EVENT_TYPE, EVENT_SOURCE, payload)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("违规存证请求已投递: violationId={}, projectId={}, source={}",
                                violation.getId(), violation.getProjectId(), source);
                    }
                });
    }

    /**
     * 组装存证内容 JSON（关键要素全量留痕，供链上溯源检索）。
     *
     * @param violation   违规记录
     * @param projectName 项目名称
     * @param source      违规来源
     * @param reporter    上报人/识别来源标识
     * @return JSON 文本
     */
    private String buildContent(ConstructionViolation violation,
                                String projectName,
                                String source,
                                String reporter) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("bizDomain", "CONSTRUCTION");
        content.put("violationId", violation.getId());
        content.put("projectId", violation.getProjectId());
        content.put("projectName", projectName);
        content.put("violationType", violation.getViolationType());
        content.put("description", violation.getDescription());
        content.put("evidenceUrl", violation.getEvidenceUrl());
        content.put("source", source);
        content.put("reporter", reporter);
        content.put("occurredAt", LocalDateTime.now().toString());
        try {
            return CONTENT_MAPPER.writeValueAsString(content);
        } catch (Exception e) {
            // 序列化兜底：不阻塞存证投递，退化为可读文本
            log.warn("存证内容JSON序列化失败, 退化为文本: violationId={}", violation.getId(), e);
            return content.toString();
        }
    }
}
