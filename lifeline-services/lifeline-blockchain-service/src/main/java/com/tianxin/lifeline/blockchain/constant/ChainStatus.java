package com.tianxin.lifeline.blockchain.constant;

/**
 * 上链状态常量。
 *
 * <p>对应 blockchain_evidence.chain_status 字段口径：0 待上链 / 1 已上链。
 */
public final class ChainStatus {

    private ChainStatus() {
    }

    /** 待上链 */
    public static final int WAITING = 0;

    /** 已上链 */
    public static final int CHAINED = 1;
}
