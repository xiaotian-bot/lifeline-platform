package com.tianxin.lifeline.plan.service;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.plan.dto.PlanTemplateQueryRequest;
import com.tianxin.lifeline.plan.dto.PlanTemplateSaveRequest;
import com.tianxin.lifeline.plan.entity.PlanTemplate;

/**
 * 应急预案模板业务服务。
 *
 * <p>提供模板分页查询（MyBatis-Plus 分页插件）、详情查询、启停切换，
 * 以及自定义模板的创建与更新（雪花 ID 手工填充）。
 */
public interface PlanTemplateService {

    /**
     * 分页查询预案模板。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<PlanTemplate> page(PlanTemplateQueryRequest request);

    /**
     * 查询模板详情。
     *
     * @param id 模板ID
     * @return 模板实体
     */
    PlanTemplate detail(Long id);

    /**
     * 启用/停用模板。
     *
     * @param id     模板ID
     * @param status 目标状态：1 启用 / 0 停用
     * @return 更新后的模板实体
     */
    PlanTemplate switchStatus(Long id, Integer status);

    /**
     * 创建自定义预案模板（雪花 ID 手工填充，缺省启用）。
     *
     * @param request 模板保存请求
     * @return 创建后的模板实体
     */
    PlanTemplate create(PlanTemplateSaveRequest request);

    /**
     * 更新预案模板（按非空字段选择性覆盖）。
     *
     * @param id      模板ID
     * @param request 模板保存请求
     * @return 更新后的模板实体
     */
    PlanTemplate update(Long id, PlanTemplateSaveRequest request);
}
