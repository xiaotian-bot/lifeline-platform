package com.tianxin.lifeline.blockchain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 区块链 DID 实体（对应表 blockchain_did，管网数字身份）。
 *
 * <p>DID 格式：{@code did:lifeline:{pipeCode}:{随机后缀}}。
 * 同一 pipeId 幂等：已注册则直接返回既有 DID，不重复生成。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blockchain_did")
public class BlockchainDid extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** DID 标识（格式 did:lifeline:{pipeCode}:{随机后缀}） */
    private String did;

    /** 关联管网ID（幂等键，idx_bd_pipe_id） */
    private Long pipeId;

    /** 管网编码 */
    private String pipeCode;
}
