package com.tianxin.lifeline.api.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 存证结果 DTO（服务间共享）。
 */
@Data
public class EvidenceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证记录ID */
    private Long evidenceId;

    /** 存证哈希 */
    private String evidenceHash;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 区块号 */
    private Long blockNo;

    /** 交易哈希 */
    private String txHash;

    /** 存证哈希算法（SM3 / SHA-256） */
    private String hashAlgorithm;

    /** 账本适配器名称（pg / fabric） */
    private String adapter;
}
