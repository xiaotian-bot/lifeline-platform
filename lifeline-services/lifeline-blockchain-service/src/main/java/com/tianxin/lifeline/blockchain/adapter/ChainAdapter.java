package com.tianxin.lifeline.blockchain.adapter;

import com.tianxin.lifeline.blockchain.entity.BlockchainEvidence;

/**
 * 区块链账本适配器接口（存证 / 验证 / 查询能力）。
 *
 * <p>适配器模式隔离底层账本实现：
 * <ul>
 *     <li>{@code PgLedgerAdapter}：PG 表模拟账本（当前默认实现）</li>
 *     <li>{@code FabricAdapter}：Hyperledger Fabric 2.5 真实链（占位，后续接入）</li>
 * </ul>
 * 通过配置 {@code lifeline.blockchain.adapter=pg|fabric} 切换，
 * 上层服务与 REST 接口契约保持不变。
 */
public interface ChainAdapter {

    /**
     * 存证上链。
     *
     * <p>调用前实体应已完成哈希计算与落库（待上链状态），
     * 适配器负责完成「上链」动作并返回回执（区块号/交易哈希/上链时间）。
     *
     * @param evidence 存证实体（已含 evidenceHash）
     * @return 上链回执
     */
    ChainReceipt deposit(BlockchainEvidence evidence);

    /**
     * 链上验证：指定存证哈希是否存在于账本且未被篡改。
     *
     * @param evidenceHash 存证哈希
     * @return true 表示链上存在且有效
     */
    boolean verifyOnChain(String evidenceHash);

    /**
     * 按存证哈希查询链上记录（溯源查询能力）。
     *
     * @param evidenceHash 存证哈希
     * @return 上链回执，不存在返回 null
     */
    ChainReceipt queryByHash(String evidenceHash);

    /**
     * 当前适配器名称（pg / fabric），用于运维观测。
     *
     * @return 适配器名称
     */
    String adapterName();
}
