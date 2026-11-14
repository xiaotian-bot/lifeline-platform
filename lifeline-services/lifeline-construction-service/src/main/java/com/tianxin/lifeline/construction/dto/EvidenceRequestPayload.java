package com.tianxin.lifeline.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证请求事件负载（生产端契约 DTO）。
 *
 * <p>字段结构与 lifeline-blockchain-service 的消费契约
 * {@code EvidenceRequestPayload} 严格一致（Jackson 按字段名反序列化）：
 * 消费端 {@code EvidenceRequestListener} 经
 * {@code envelope.convertPayload(EvidenceRequestPayload.class)} 还原，
 * 字段名/类型任何偏差都会导致负载转换失败，禁止单独修改本类字段。
 */
@Data
public class EvidenceRequestPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证类型（施工域固定 CONSTRUCTION） */
    private String evidenceType;

    /** 关联业务对象ID（溯源聚合键，施工域为施工项目ID） */
    private Long relatedId;

    /** 关联业务对象类型（施工域固定 CONSTRUCTION_PROJECT） */
    private String relatedType;

    /** 存证内容（JSON 文本，描述违规环节关键要素） */
    private String content;

    /** 管网编码（预留字段，施工域置空） */
    private String pipeCode;
}
