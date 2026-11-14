package com.tianxin.lifeline.plan.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预案模板分页查询请求。
 *
 * <p>支持模板名称关键字 / 模板类型 / 场景类型 / 状态组合过滤，
 * 默认按创建时间降序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanTemplateQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称关键字（模糊匹配） */
    private String keyword;

    /** 模板类型：EMERGENCY 应急处置预案 */
    private String templateType;

    /** 场景类型：GAS_LEAK/WATER_BURST/HEATING_LEAK/DRAINAGE_OVERFLOW/
     * ROAD_COLLAPSE/THIRD_PARTY_DAMAGE/CORROSION_FAILURE/COMPREHENSIVE */
    private String scenarioType;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
