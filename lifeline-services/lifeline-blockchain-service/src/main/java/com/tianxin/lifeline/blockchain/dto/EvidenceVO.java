package com.tianxin.lifeline.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 存证结果视图（存证上链后返回）。
 */
@Data
public class EvidenceVO implements Serializable {

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

    /** 区块号（PG 模拟账本为自增高度；Fabric 接入后为真实区块高度） */
    private Long blockNo;

    /** 交易哈希（PG 模拟账本为模拟 TxID；Fabric 接入后为真实 TxID） */
    private String txHash;

    /** 存证哈希算法（SM3 / SHA-256） */
    private String hashAlgorithm;

    /** 账本适配器名称（pg / fabric） */
    private String adapter;
}
