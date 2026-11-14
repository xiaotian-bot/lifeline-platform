package com.tianxin.lifeline.blockchain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 区块链存证实体（对应表 blockchain_evidence）。
 *
 * <p>存证哈希口径：{@code hash(content + relatedId + relatedType + 上链时间戳毫秒)}，
 * 优先国密 SM3（BouncyCastle），缺失时降级 SHA-256。
 * 防篡改校验即按同一口径重算哈希并与 evidence_hash 比对。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blockchain_evidence")
public class BlockchainEvidence extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证哈希（SM3/SHA-256 十六进制，idx_be_hash） */
    private String evidenceHash;

    /** 存证类型（如 ORDER 工单 / CONSTRUCTION 施工 / PLAN 预案 / WARNING 预警等，由生产方约定） */
    private String evidenceType;

    /** 关联业务对象ID（溯源时间线按该字段聚合，如管网ID/工单ID） */
    private Long relatedId;

    /** 关联业务对象类型（如 PIPE 管网 / ORDER 工单） */
    private String relatedType;

    /** 存证内容（JSON 文本，描述业务环节关键要素） */
    private String content;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;

    /** 上链时间（同时作为存证哈希的时间戳因子，校验时按此重算） */
    private LocalDateTime chainTime;
}
