package com.tianxin.lifeline.construction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 施工违规记录实体（对应表 construction_violation，区块链存证）。
 *
 * <p>违规由 AI 视觉识别命中或人工上报产生，产生即经
 * EventEnvelope 投递 pipe_evidence_request 触发存证上链，
 * blockchain_hash 由后续存证回执/对账回填。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("construction_violation")
public class ConstructionViolation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属施工项目ID */
    private Long projectId;

    /** 违规类型：NO_HELMET 未戴安全帽 / NO_FENCE 围挡缺失 / UNAUTHORIZED_EXCAVATION 擅自开挖 等 */
    private String violationType;

    /** 违规描述 */
    private String description;

    /** 证据影像对象键（MinIO 对象地址） */
    private String evidenceUrl;

    /** 处理状态：0 待处理 / 1 已处理 */
    private Integer status;

    /** 区块链存证哈希（存证回执回填，可空） */
    private String blockchainHash;
}
