package com.tianxin.lifeline.blockchain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.blockchain.entity.BlockchainDid;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 区块链 DID Mapper（表 blockchain_did）。
 */
@Mapper
public interface BlockchainDidMapper extends BaseMapper<BlockchainDid> {

    /**
     * 按管网ID查询有效 DID（幂等判断，deleted=0）。
     *
     * @param pipeId 管网ID
     * @return DID 记录，不存在返回 null
     */
    BlockchainDid selectByPipeId(@Param("pipeId") Long pipeId);

    /**
     * 按 DID 标识查询记录（溯源入口，deleted=0）。
     *
     * @param did DID 标识
     * @return DID 记录，不存在返回 null
     */
    BlockchainDid selectByDid(@Param("did") String did);
}
