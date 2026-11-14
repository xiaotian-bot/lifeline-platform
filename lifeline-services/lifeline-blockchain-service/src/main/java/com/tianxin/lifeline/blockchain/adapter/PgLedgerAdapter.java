package com.tianxin.lifeline.blockchain.adapter;

import com.tianxin.lifeline.blockchain.constant.BlockchainRedisKeys;
import com.tianxin.lifeline.blockchain.constant.ChainStatus;
import com.tianxin.lifeline.blockchain.entity.BlockchainEvidence;
import com.tianxin.lifeline.blockchain.mapper.BlockchainEvidenceMapper;
import com.tianxin.lifeline.blockchain.util.HashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * PG 模拟账本适配器（默认实现）。
 *
 * <p>开发阶段以 blockchain_evidence 表模拟链上账本，接口契约与真实链一致：
 * <ul>
 *     <li>存证：chain_status 直接置「已上链」，落 chain_time</li>
 *     <li>区块号：Redis INCR 模拟区块高度自增（Redis 不可用时以存量条数兜底）</li>
 *     <li>交易哈希：存证哈希前 16 位 + 随机段，模拟 TxID 形态</li>
 *     <li>验证/查询：按 evidence_hash 命中 PG 账本</li>
 * </ul>
 * 启用条件：{@code lifeline.blockchain.adapter=pg}（缺省即启用）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "lifeline.blockchain", name = "adapter", havingValue = "pg", matchIfMissing = true)
public class PgLedgerAdapter implements ChainAdapter {

    /** 模拟交易哈希前缀 */
    private static final String TX_HASH_PREFIX = "0x";

    /** Redis 模板（区块高度自增） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 存证 Mapper（验证/查询） */
    private final BlockchainEvidenceMapper evidenceMapper;

    /**
     * 构造适配器。
     *
     * @param stringRedisTemplate Redis 模板
     * @param evidenceMapper      存证 Mapper
     */
    public PgLedgerAdapter(StringRedisTemplate stringRedisTemplate,
                           BlockchainEvidenceMapper evidenceMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.evidenceMapper = evidenceMapper;
    }

    /**
     * 模拟上链：直接置已上链并返回模拟回执。
     *
     * @param evidence 存证实体（已含 evidenceHash）
     * @return 上链回执（模拟区块号与交易哈希）
     */
    @Override
    public ChainReceipt deposit(BlockchainEvidence evidence) {
        LocalDateTime chainTime = evidence.getChainTime() != null ? evidence.getChainTime() : LocalDateTime.now();
        return ChainReceipt.builder()
                .blockNo(nextBlockNo())
                .txHash(mockTxHash(evidence.getEvidenceHash()))
                .chainStatus(ChainStatus.CHAINED)
                .chainTime(chainTime)
                .hashAlgorithm(HashUtil.algorithm())
                .build();
    }

    /**
     * 验证存证哈希是否存在于 PG 模拟账本。
     *
     * @param evidenceHash 存证哈希
     * @return true 表示账本中存在
     */
    @Override
    public boolean verifyOnChain(String evidenceHash) {
        return evidenceMapper.selectByEvidenceHash(evidenceHash) != null;
    }

    /**
     * 按存证哈希查询 PG 模拟账本记录并组装回执。
     *
     * @param evidenceHash 存证哈希
     * @return 上链回执，不存在返回 null
     */
    @Override
    public ChainReceipt queryByHash(String evidenceHash) {
        BlockchainEvidence evidence = evidenceMapper.selectByEvidenceHash(evidenceHash);
        if (evidence == null) {
            return null;
        }
        return ChainReceipt.builder()
                .blockNo(evidence.getId())
                .txHash(mockTxHash(evidence.getEvidenceHash()))
                .chainStatus(evidence.getChainStatus())
                .chainTime(evidence.getChainTime())
                .hashAlgorithm(HashUtil.algorithm())
                .build();
    }

    /**
     * 适配器名称。
     *
     * @return pg
     */
    @Override
    public String adapterName() {
        return "pg";
    }

    /**
     * 模拟区块号自增（Redis INCR；Redis 异常时以存量条数 +1 兜底）。
     *
     * @return 区块号
     */
    private Long nextBlockNo() {
        try {
            Long height = stringRedisTemplate.opsForValue().increment(BlockchainRedisKeys.BLOCK_HEIGHT);
            if (height != null) {
                return height;
            }
        } catch (Exception e) {
            log.warn("Redis 区块高度自增失败, 降级使用存量条数兜底", e);
        }
        return evidenceMapper.selectCount(null) + 1;
    }

    /**
     * 构造模拟交易哈希：{@code 0x + 存证哈希前16位 + 16位随机十六进制}。
     *
     * @param evidenceHash 存证哈希
     * @return 模拟交易哈希
     */
    private String mockTxHash(String evidenceHash) {
        String prefix = evidenceHash == null ? "" : evidenceHash.substring(0, Math.min(16, evidenceHash.length()));
        return TX_HASH_PREFIX + prefix + Long.toHexString(ThreadLocalRandom.current().nextLong())
                + Long.toHexString(ThreadLocalRandom.current().nextLong());
    }
}
