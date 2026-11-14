package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 施工工序 DTO（服务间共享）。
 *
 * <p>镜像 construction-service 的 ConstructionProcess 实体，供跨服务调用。
 */
@Data
public class ConstructionProcessDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工序ID */
    private Long id;

    /** 所属施工项目ID */
    private Long projectId;

    /** 工序名称 */
    private String processName;

    /** 工序类型：EXCAVATION 开挖 / PIPE_LAYING 管道铺设 等 */
    private String processType;

    /** 质检状态：0 待质检 / 1 质检中 / 2 质检通过 / 3 质检不通过 */
    private Integer status;

    /** 质检人 */
    private String inspector;

    /** 质检时间 */
    private LocalDateTime inspectTime;

    /** 质检得分（百分制） */
    private BigDecimal qualityScore;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
