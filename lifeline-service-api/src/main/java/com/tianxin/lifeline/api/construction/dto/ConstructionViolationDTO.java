package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 施工违规记录 DTO（服务间共享）。
 *
 * <p>镜像 construction-service 的 ConstructionViolation 实体，供跨服务调用。
 */
@Data
public class ConstructionViolationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 违规记录ID */
    private Long id;

    /** 所属施工项目ID */
    private Long projectId;

    /** 违规类型：NO_HELMET / NO_FENCE / UNAUTHORIZED_EXCAVATION 等 */
    private String violationType;

    /** 违规描述 */
    private String description;

    /** 证据影像对象键（MinIO 对象地址） */
    private String evidenceUrl;

    /** 处理状态：0 待处理 / 1 已处理 */
    private Integer status;

    /** 区块链存证哈希 */
    private String blockchainHash;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
