package com.tianxin.lifeline.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证防篡改校验结果视图。
 *
 * <p>按存证口径 {@code hash(content | relatedId | relatedType | 时间戳)} 重算哈希，
 * 与落库 evidence_hash 比对，不一致即判定内容被篡改。
 */
@Data
public class EvidenceVerifyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证记录ID */
    private Long evidenceId;

    /** 是否被篡改（true=重算哈希与落库哈希不一致） */
    private Boolean tampered;

    /** 落库存证哈希（上链时记录） */
    private String storedHash;

    /** 重算存证哈希（按当前内容重算） */
    private String recomputedHash;

    /** 存证哈希算法（SM3 / SHA-256） */
    private String hashAlgorithm;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;
}
