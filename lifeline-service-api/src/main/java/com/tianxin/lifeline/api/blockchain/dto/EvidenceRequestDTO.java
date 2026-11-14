package com.tianxin.lifeline.api.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证请求 DTO（服务间共享）。
 */
@Data
public class EvidenceRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证类型（如 ORDER / CONSTRUCTION / PLAN / WARNING） */
    private String evidenceType;

    /** 关联业务对象ID */
    private Long relatedId;

    /** 关联业务对象类型（如 PIPE / ORDER） */
    private String relatedType;

    /** 存证内容 */
    private String content;
}
