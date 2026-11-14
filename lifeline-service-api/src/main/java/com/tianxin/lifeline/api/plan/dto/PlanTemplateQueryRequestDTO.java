package com.tianxin.lifeline.api.plan.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预案模板分页查询请求 DTO（服务间共享）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlanTemplateQueryRequestDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称关键字（模糊匹配） */
    private String keyword;

    /** 模板类型 */
    private String templateType;

    /** 场景类型 */
    private String scenarioType;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
