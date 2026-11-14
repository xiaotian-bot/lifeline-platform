package com.tianxin.lifeline.blockchain.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上链回执。
 *
 * <p>无论底层是 PG 模拟账本还是真实 Fabric 链，统一返回该结构，
 * 保证上层接口契约稳定（后续切换 Fabric 2.5 无需变更调用方）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainReceipt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 区块号（PG 模拟账本下为自增高度；Fabric 下对应区块高度） */
    private Long blockNo;

    /** 交易哈希（PG 模拟账本下为存证哈希前缀 + 随机段；Fabric 下为真实 TxID） */
    private String txHash;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;

    /** 上链时间 */
    private LocalDateTime chainTime;

    /** 存证哈希算法（SM3 / SHA-256） */
    private String hashAlgorithm;
}
