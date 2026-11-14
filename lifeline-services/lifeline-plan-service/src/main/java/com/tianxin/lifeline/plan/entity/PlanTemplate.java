package com.tianxin.lifeline.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 应急预案模板实体（对应表 plan_template）。
 *
 * <p>八大类场景模板种子见 deploy/sql/07-seed-p9.sql（id 9001-9008）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_template")
public class PlanTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称 */
    private String templateName;

    /** 模板类型：EMERGENCY 应急处置预案 */
    private String templateType;

    /** 场景类型：GAS_LEAK/WATER_BURST/HEATING_LEAK/DRAINAGE_OVERFLOW/
     * ROAD_COLLAPSE/THIRD_PARTY_DAMAGE/CORROSION_FAILURE/COMPREHENSIVE */
    private String scenarioType;

    /** 预案内容（处置要点文本） */
    private String content;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
