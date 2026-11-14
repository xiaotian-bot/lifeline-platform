package com.tianxin.lifeline.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证请求事件负载（Kafka 消费契约 DTO）。
 *
 * <p><b>生产方约定</b>：operation-service（运维工单/处置环节）、
 * construction-service（施工环节）、plan-service（预案环节）三个服务
 * 向 Topic {@code pipe_evidence_request}（{@code TopicConstants.PIPE_EVIDENCE_REQUEST}）
 * 发送 {@code EventEnvelope} 信封时，payload 必须遵循本 DTO 字段结构：
 * <pre>
 * {
 *   "evidenceType": "ORDER / CONSTRUCTION / PLAN 等存证类型",
 *   "relatedId":    关联业务对象ID（如管网ID，溯源聚合键）,
 *   "relatedType":  "PIPE / ORDER 等关联对象类型",
 *   "content":      "存证内容（建议 JSON 文本）",
 *   "pipeCode":     "管网编码（预留，供 DID 关联与扩展使用）"
 * }
 * </pre>
 * 信封 eventId 用于消费端 Redis SETNX 幂等去重，重复投递自动拦截。
 */
@Data
public class EvidenceRequestPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证类型（如 ORDER 工单 / CONSTRUCTION 施工 / PLAN 预案） */
    private String evidenceType;

    /** 关联业务对象ID（溯源聚合键，如管网ID/工单ID） */
    private Long relatedId;

    /** 关联业务对象类型（如 PIPE 管网 / ORDER 工单） */
    private String relatedType;

    /** 存证内容（建议 JSON 文本，描述业务环节关键要素） */
    private String content;

    /** 管网编码（预留字段，供 DID 关联与后续扩展使用） */
    private String pipeCode;
}
