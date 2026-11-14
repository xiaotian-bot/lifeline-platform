package com.tianxin.lifeline.blockchain.constant;

/**
 * 区块链服务 Redis 键常量。
 *
 * <p>统一前缀 {@code blockchain:}，避免与其他服务键冲突。
 */
public final class BlockchainRedisKeys {

    private BlockchainRedisKeys() {
    }

    /** 存证请求事件幂等去重键前缀（eventId 级 SETNX） */
    private static final String EVIDENCE_IDEMPOTENT_PREFIX = "blockchain:evidence:idem:";

    /** 模拟账本区块高度键（PG 模拟账本下用 Redis INCR 模拟区块号自增） */
    public static final String BLOCK_HEIGHT = "blockchain:chain:block-height";

    /**
     * 构造存证请求事件幂等键。
     *
     * @param eventId 事件信封 eventId
     * @return 幂等键
     */
    public static String evidenceIdempotent(String eventId) {
        return EVIDENCE_IDEMPOTENT_PREFIX + eventId;
    }
}
