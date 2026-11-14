package com.tianxin.lifeline.construction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 施工工序实体（对应表 construction_process）。
 *
 * <p>承载工序质检记录：质检人/质检时间/质检得分随人工质检与
 * AI 视觉质检结果回写，status 表示质检状态（0 待质检 / 1 质检中 /
 * 2 质检通过 / 3 质检不通过）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("construction_process")
public class ConstructionProcess extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属施工项目ID */
    private Long projectId;

    /** 工序名称 */
    private String processName;

    /** 工序类型：EXCAVATION 开挖 / PIPE_LAYING 管道铺设 等 */
    private String processType;

    /** 质检状态：0 待质检 / 1 质检中 / 2 质检通过 / 3 质检不通过 */
    private Integer status;

    /** 质检人（AI 视觉质检记为 AI-VISION:摄像头ID） */
    private String inspector;

    /** 质检时间 */
    private LocalDateTime inspectTime;

    /** 质检得分（百分制） */
    private BigDecimal qualityScore;
}
