package com.tianxin.lifeline.api.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 存证时间线单环节 DTO（服务间共享）。
 */
@Data
public class EvidenceTimelineItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证记录ID */
    private Long evidenceId;

    /** 存证哈希 */
    private String evidenceHash;

    /** 存证类型 */
    private String evidenceType;

    /** 关联业务对象ID */
    private Long relatedId;

    /** 关联业务对象类型 */
    private String relatedType;

    /** 存证内容 */
    private String content;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;

    /** 上链时间 */
    private LocalDateTime chainTime;
}
