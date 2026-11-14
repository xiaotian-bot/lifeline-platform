package com.tianxin.lifeline.blockchain.adapter;

import com.tianxin.lifeline.blockchain.entity.BlockchainEvidence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Hyperledger Fabric 2.5 账本适配器（占位实现）。
 *
 * <p>启用条件：{@code lifeline.blockchain.adapter=fabric}。
 *
 * <p>后续接入规划（P9 批次B 或专项迭代）：
 * <ul>
 *     <li>引入 fabric-gateway（org.hyperledger.fabric:fabric-gateway）客户端 SDK，
 *         对接 Fabric 2.5 网络（CA 签发证书 + Gateway 连接）</li>
 *     <li>{@link #deposit}：调用存证链码（chaincode）提交交易，
 *         区块号/交易哈希取 Fabric 提交回执（BlockMetadata / TxID）</li>
 *     <li>{@link #verifyOnChain}：按存证哈希查询链上状态（query state），
 *         并与链上哈希比对</li>
 *     <li>{@link #queryByHash}：链上状态查询 + 历史溯源（GetHistoryForKey）</li>
 * </ul>
 * 当前为占位实现，调用即抛出 {@link UnsupportedOperationException}，
 * 防止在未接链时被误用；生产默认使用 {@code PgLedgerAdapter}。
 */
@Component
@ConditionalOnProperty(prefix = "lifeline.blockchain", name = "adapter", havingValue = "fabric")
public class FabricAdapter implements ChainAdapter {

    /** 未接链提示 */
    private static final String NOT_CONNECTED =
            "Fabric 2.5 账本尚未接入，请配置 lifeline.blockchain.adapter=pg 使用 PG 模拟账本，"
                    + "或按 FabricAdapter 类注释完成 fabric-gateway SDK 对接";

    /**
     * 存证上链（占位）。
     *
     * @param evidence 存证实体
     * @return 不会返回，直接抛出异常
     */
    @Override
    public ChainReceipt deposit(BlockchainEvidence evidence) {
        throw new UnsupportedOperationException(NOT_CONNECTED);
    }

    /**
     * 链上验证（占位）。
     *
     * @param evidenceHash 存证哈希
     * @return 不会返回，直接抛出异常
     */
    @Override
    public boolean verifyOnChain(String evidenceHash) {
        throw new UnsupportedOperationException(NOT_CONNECTED);
    }

    /**
     * 链上查询（占位）。
     *
     * @param evidenceHash 存证哈希
     * @return 不会返回，直接抛出异常
     */
    @Override
    public ChainReceipt queryByHash(String evidenceHash) {
        throw new UnsupportedOperationException(NOT_CONNECTED);
    }

    /**
     * 适配器名称。
     *
     * @return fabric
     */
    @Override
    public String adapterName() {
        return "fabric";
    }
}
