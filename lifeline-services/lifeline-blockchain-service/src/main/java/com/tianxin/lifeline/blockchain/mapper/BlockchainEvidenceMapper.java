package com.tianxin.lifeline.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.blockchain.entity.BlockchainEvidence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 区块链存证 Mapper（表 blockchain_evidence）。
 */
@Mapper
public interface BlockchainEvidenceMapper extends BaseMapper<BlockchainEvidence> {

    /**
     * 按关联业务对象ID查询存证时间线（上链时间倒序，deleted=0）。
     *
     * @param relatedId 关联业务对象ID（如管网ID）
     * @return 存证列表（时间倒序）
     */
    List<BlockchainEvidence> selectTimelineByRelatedId(@Param("relatedId") Long relatedId);

    /**
     * 按存证哈希查询记录（deleted=0，取首条）。
     *
     * @param evidenceHash 存证哈希
     * @return 存证记录，不存在返回 null
     */
    BlockchainEvidence selectByEvidenceHash(@Param("evidenceHash") String evidenceHash);
}
