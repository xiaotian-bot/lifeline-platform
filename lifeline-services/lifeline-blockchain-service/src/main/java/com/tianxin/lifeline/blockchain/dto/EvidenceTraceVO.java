package com.tianxin.lifeline.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 管道存证溯源视图（按 DID 聚合的全环节存证时间线）。
 */
@Data
public class EvidenceTraceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** DID 标识 */
    private String did;

    /** 管网ID */
    private Long pipeId;

    /** 管网编码 */
    private String pipeCode;

    /** 存证环节总数 */
    private Integer total;

    /** 存证时间线（按上链时间倒序） */
    private List<EvidenceTimelineItem> timeline;
}
