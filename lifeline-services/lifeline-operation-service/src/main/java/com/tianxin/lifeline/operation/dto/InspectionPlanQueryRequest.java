package com.tianxin.lifeline.operation.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 巡检计划分页查询请求。
 *
 * <p>支持计划类型 / 区域 / 状态过滤，默认按创建时间降序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionPlanQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划名称（模糊匹配） */
    private String planName;

    /** 计划类型 */
    private String planType;

    /** 所属区域编码 */
    private String areaId;

    /** 计划状态：1 启用 / 0 停用 */
    private Integer status;
}
