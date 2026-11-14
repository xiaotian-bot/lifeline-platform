package com.tianxin.lifeline.blockchain.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tianxin.lifeline.blockchain.adapter.ChainAdapter;
import com.tianxin.lifeline.blockchain.adapter.ChainReceipt;
import com.tianxin.lifeline.blockchain.constant.ChainStatus;
import com.tianxin.lifeline.blockchain.dto.EvidenceRequest;
import com.tianxin.lifeline.blockchain.dto.EvidenceRequestPayload;
import com.tianxin.lifeline.blockchain.dto.EvidenceTimelineItem;
import com.tianxin.lifeline.blockchain.dto.EvidenceTraceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVerifyVO;
import com.tianxin.lifeline.blockchain.entity.BlockchainDid;
import com.tianxin.lifeline.blockchain.entity.BlockchainEvidence;
import com.tianxin.lifeline.blockchain.mapper.BlockchainDidMapper;
import com.tianxin.lifeline.blockchain.mapper.BlockchainEvidenceMapper;
import com.tianxin.lifeline.blockchain.service.EvidenceService;
import com.tianxin.lifeline.blockchain.util.HashUtil;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 区块链存证业务服务实现。
 *
 * <p>存证流程：计算哈希 → 待上链落库 → 经 {@link ChainAdapter} 上链
 * （默认 PG 模拟账本直接置已上链）→ 回填上链状态。
 * 哈希口径：{@code hash(content | relatedId | relatedType | 上链时间戳毫秒)}，
 * 防篡改校验按同一口径重算比对。
 */
@Slf4j
@Service
public class EvidenceServiceImpl implements EvidenceService {

    /** 存证 Mapper */
    private final BlockchainEvidenceMapper evidenceMapper;

    /** DID Mapper（溯源入口） */
    private final BlockchainDidMapper didMapper;

    /** 区块链账本适配器（pg / fabric） */
    private final ChainAdapter chainAdapter;

    /**
     * 构造服务。
     *
     * @param evidenceMapper 存证 Mapper
     * @param didMapper      DID Mapper
     * @param chainAdapter   账本适配器
     */
    public EvidenceServiceImpl(BlockchainEvidenceMapper evidenceMapper,
                               BlockchainDidMapper didMapper,
                               ChainAdapter chainAdapter) {
        this.evidenceMapper = evidenceMapper;
        this.didMapper = didMapper;
        this.chainAdapter = chainAdapter;
    }

    /**
     * REST 存证入口。
     *
     * @param request 存证请求
     * @return 存证结果
     */
    @Override
    public EvidenceVO deposit(EvidenceRequest request) {
        return doDeposit(request.getEvidenceType(), request.getRelatedId(),
                request.getRelatedType(), request.getContent());
    }

    /**
     * Kafka 事件存证入口（生产方契约负载）。
     *
     * @param payload 存证请求事件负载
     * @return 存证结果
     */
    @Override
    public EvidenceVO depositFromPayload(EvidenceRequestPayload payload) {
        if (StrUtil.isBlank(payload.getEvidenceType()) || payload.getRelatedId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "存证事件负载缺少 evidenceType 或 relatedId");
        }
        String relatedType = StrUtil.blankToDefault(payload.getRelatedType(), "PIPE");
        return doDeposit(payload.getEvidenceType(), payload.getRelatedId(), relatedType, payload.getContent());
    }

    /**
     * 按 DID 溯源：DID → 管网ID → 按 related_id 聚合全环节存证时间线（时间倒序）。
     *
     * @param did DID 标识
     * @return 溯源时间线视图
     */
    @Override
    public EvidenceTraceVO trace(String did) {
        BlockchainDid didRecord = didMapper.selectByDid(did);
        if (didRecord == null) {
            throw new BizException(ErrorCode.DID_NOT_FOUND);
        }

        List<BlockchainEvidence> evidences = evidenceMapper.selectTimelineByRelatedId(didRecord.getPipeId());
        List<EvidenceTimelineItem> timeline = evidences.stream().map(this::toTimelineItem).toList();

        EvidenceTraceVO vo = new EvidenceTraceVO();
        vo.setDid(didRecord.getDid());
        vo.setPipeId(didRecord.getPipeId());
        vo.setPipeCode(didRecord.getPipeCode());
        vo.setTotal(timeline.size());
        vo.setTimeline(timeline);
        return vo;
    }

    /**
     * 防篡改校验：重算哈希与落库哈希比对。
     *
     * @param evidenceId 存证记录ID
     * @return 校验结果
     */
    @Override
    public EvidenceVerifyVO verify(Long evidenceId) {
        BlockchainEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "存证记录不存在");
        }

        String recomputed = HashUtil.evidenceHash(evidence.getContent(), evidence.getRelatedId(),
                evidence.getRelatedType(), toEpochMilli(evidence.getChainTime()));

        EvidenceVerifyVO vo = new EvidenceVerifyVO();
        vo.setEvidenceId(evidence.getId());
        vo.setTampered(!Objects.equals(recomputed, evidence.getEvidenceHash()));
        vo.setStoredHash(evidence.getEvidenceHash());
        vo.setRecomputedHash(recomputed);
        vo.setHashAlgorithm(HashUtil.algorithm());
        vo.setChainStatus(evidence.getChainStatus());
        if (Boolean.TRUE.equals(vo.getTampered())) {
            log.warn("存证防篡改校验发现不一致: evidenceId={}, stored={}, recomputed={}",
                    evidenceId, evidence.getEvidenceHash(), recomputed);
        }
        return vo;
    }

    /**
     * 存证核心流程：哈希计算 → 待上链落库 → 适配器上链 → 回填状态。
     *
     * @param evidenceType 存证类型
     * @param relatedId    关联业务对象ID
     * @param relatedType  关联业务对象类型
     * @param content      存证内容
     * @return 存证结果
     */
    private EvidenceVO doDeposit(String evidenceType, Long relatedId, String relatedType, String content) {
        LocalDateTime chainTime = LocalDateTime.now();

        BlockchainEvidence entity = new BlockchainEvidence();
        entity.setEvidenceType(evidenceType);
        entity.setRelatedId(relatedId);
        entity.setRelatedType(relatedType);
        entity.setContent(content);
        entity.setChainTime(chainTime);
        entity.setChainStatus(ChainStatus.WAITING);
        entity.setEvidenceHash(HashUtil.evidenceHash(content, relatedId, relatedType, toEpochMilli(chainTime)));

        // 经账本适配器上链（PG 模拟账本直接返回已上链回执）
        ChainReceipt receipt = chainAdapter.deposit(entity);
        entity.setChainStatus(receipt.getChainStatus());
        if (receipt.getChainTime() != null && !receipt.getChainTime().equals(chainTime)) {
            // 上链时间被适配器修正时按新时间重算哈希，保证校验口径一致
            entity.setChainTime(receipt.getChainTime());
            entity.setEvidenceHash(HashUtil.evidenceHash(content, relatedId, relatedType,
                    toEpochMilli(receipt.getChainTime())));
        }
        evidenceMapper.insert(entity);
        log.info("存证上链成功: evidenceId={}, type={}, hash={}, adapter={}",
                entity.getId(), evidenceType, entity.getEvidenceHash(), chainAdapter.adapterName());

        EvidenceVO vo = new EvidenceVO();
        vo.setEvidenceId(entity.getId());
        vo.setEvidenceHash(entity.getEvidenceHash());
        vo.setChainStatus(entity.getChainStatus());
        vo.setChainTime(entity.getChainTime());
        vo.setBlockNo(receipt.getBlockNo());
        vo.setTxHash(receipt.getTxHash());
        vo.setHashAlgorithm(HashUtil.algorithm());
        vo.setAdapter(chainAdapter.adapterName());
        return vo;
    }

    /**
     * 存证实体转时间线条目。
     *
     * @param evidence 存证实体
     * @return 时间线条目
     */
    private EvidenceTimelineItem toTimelineItem(BlockchainEvidence evidence) {
        EvidenceTimelineItem item = new EvidenceTimelineItem();
        item.setEvidenceId(evidence.getId());
        item.setEvidenceHash(evidence.getEvidenceHash());
        item.setEvidenceType(evidence.getEvidenceType());
        item.setRelatedId(evidence.getRelatedId());
        item.setRelatedType(evidence.getRelatedType());
        item.setContent(evidence.getContent());
        item.setChainStatus(evidence.getChainStatus());
        item.setChainTime(evidence.getChainTime());
        return item;
    }

    /**
     * LocalDateTime 转毫秒时间戳（系统时区，null 视为 0）。
     *
     * @param time 时间
     * @return 毫秒时间戳
     */
    private long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return 0L;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<Map<String, Object>> pageEvidence(int page, int size) {
        List<BlockchainEvidence> all = evidenceMapper.selectList(null);
        long total = all.size();
        int offset = (page - 1) * size;
        List<BlockchainEvidence> pageList = all.stream()
                .skip(offset)
                .limit(size)
                .toList();

        List<Map<String, Object>> records = new ArrayList<>();
        for (BlockchainEvidence e : pageList) {
            records.add(toEvidenceMap(e));
        }
        return PageResult.of(records, total, page, size);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getEvidenceById(Long evidenceId) {
        BlockchainEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "存证记录不存在");
        }
        return toEvidenceMap(evidence);
    }

    /**
     * 存证实体转前端展示 Map。
     */
    private Map<String, Object> toEvidenceMap(BlockchainEvidence e) {
        Map<String, Object> map = new HashMap<>();
        map.put("evidenceId", e.getId());
        map.put("eventType", e.getEvidenceType());
        map.put("relatedId", e.getRelatedId());
        map.put("relatedType", e.getRelatedType());
        map.put("content", e.getContent());
        map.put("chainStatus", e.getChainStatus());
        map.put("chainTime", e.getChainTime() != null ? e.getChainTime().toString() : null);
        map.put("evidenceHash", e.getEvidenceHash());
        map.put("confirmed", e.getChainStatus() != null && e.getChainStatus() == 1);
        map.put("txHash", e.getEvidenceHash() != null ? e.getEvidenceHash() : "");
        map.put("blockNumber", e.getId());
        map.put("pipeId", e.getRelatedId());
        map.put("operator", "system");
        map.put("timestamp", e.getChainTime() != null ? e.getChainTime().toString() : null);
        return map;
    }
}
