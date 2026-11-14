package com.tianxin.lifeline.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.plan.dto.PlanTemplateQueryRequest;
import com.tianxin.lifeline.plan.dto.PlanTemplateSaveRequest;
import com.tianxin.lifeline.plan.entity.PlanTemplate;
import com.tianxin.lifeline.plan.mapper.PlanTemplateMapper;
import com.tianxin.lifeline.plan.service.PlanTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 应急预案模板业务服务实现。
 *
 * <p>分页基于 MyBatis-Plus 分页插件（common-data 已注册
 * PaginationInnerInterceptor），支持关键字/类型/场景/状态过滤，
 * 默认创建时间降序。
 */
@Slf4j
@Service
public class PlanTemplateServiceImpl implements PlanTemplateService {

    /** 模板 Mapper */
    private final PlanTemplateMapper templateMapper;

    /**
     * 构造服务（构造器注入）。
     *
     * @param templateMapper 模板 Mapper
     */
    public PlanTemplateServiceImpl(PlanTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    /**
     * 分页查询预案模板。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<PlanTemplate> page(PlanTemplateQueryRequest request) {
        LambdaQueryWrapper<PlanTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(PlanTemplate::getTemplateName, request.getKeyword().trim());
        }
        if (StringUtils.hasText(request.getTemplateType())) {
            wrapper.eq(PlanTemplate::getTemplateType, request.getTemplateType());
        }
        if (StringUtils.hasText(request.getScenarioType())) {
            wrapper.eq(PlanTemplate::getScenarioType, request.getScenarioType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(PlanTemplate::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(PlanTemplate::getCreatedAt);

        Page<PlanTemplate> page = templateMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), request.getPage(), request.getSize());
    }

    /**
     * 查询模板详情，不存在抛业务异常。
     *
     * @param id 模板ID
     * @return 模板实体
     */
    @Override
    public PlanTemplate detail(Long id) {
        return loadTemplate(id);
    }

    /**
     * 启用/停用模板（status 仅允许 1 或 0）。
     *
     * @param id     模板ID
     * @param status 目标状态
     * @return 更新后的模板实体
     */
    @Override
    public PlanTemplate switchStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "模板状态取值不合法，允许值：1 启用 / 0 停用");
        }
        PlanTemplate template = loadTemplate(id);
        template.setStatus(status);
        templateMapper.updateById(template);
        log.info("预案模板状态切换: id={}, status={}", id, status == 1 ? "启用" : "停用");
        return template;
    }

    /**
     * 创建自定义预案模板（雪花 ID 手工填充，模板类型缺省
     * EMERGENCY、状态缺省 1 启用）。
     *
     * @param request 模板保存请求
     * @return 创建后的模板实体
     */
    @Override
    public PlanTemplate create(PlanTemplateSaveRequest request) {
        PlanTemplate template = new PlanTemplate();
        template.setId(SnowflakeIdGenerator.getInstance().nextId());
        template.setTemplateName(request.getTemplateName().trim());
        template.setTemplateType(StringUtils.hasText(request.getTemplateType())
                ? request.getTemplateType().trim() : "EMERGENCY");
        template.setScenarioType(StringUtils.hasText(request.getScenarioType())
                ? request.getScenarioType().trim() : null);
        template.setContent(request.getContent());
        template.setStatus(request.getStatus() == null ? 1 : validateStatus(request.getStatus()));
        templateMapper.insert(template);
        log.info("创建自定义预案模板: id={}, name={}, scenarioType={}",
                template.getId(), template.getTemplateName(), template.getScenarioType());
        return template;
    }

    /**
     * 更新预案模板（按非空字段选择性覆盖，不允许修改主键）。
     *
     * @param id      模板ID
     * @param request 模板保存请求
     * @return 更新后的模板实体
     */
    @Override
    public PlanTemplate update(Long id, PlanTemplateSaveRequest request) {
        PlanTemplate template = loadTemplate(id);
        template.setTemplateName(request.getTemplateName().trim());
        if (StringUtils.hasText(request.getTemplateType())) {
            template.setTemplateType(request.getTemplateType().trim());
        }
        if (StringUtils.hasText(request.getScenarioType())) {
            template.setScenarioType(request.getScenarioType().trim());
        }
        template.setContent(request.getContent());
        if (request.getStatus() != null) {
            template.setStatus(validateStatus(request.getStatus()));
        }
        templateMapper.updateById(template);
        log.info("更新预案模板: id={}, name={}", id, template.getTemplateName());
        return template;
    }

    /**
     * 校验状态取值（仅允许 1 启用 / 0 停用），非法抛业务异常。
     *
     * @param status 目标状态
     * @return 原值透传
     */
    private Integer validateStatus(Integer status) {
        if (status != 0 && status != 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "模板状态取值不合法，允许值：1 启用 / 0 停用");
        }
        return status;
    }

    /**
     * 加载模板，不存在抛业务异常。
     *
     * @param id 模板ID
     * @return 模板实体
     */
    private PlanTemplate loadTemplate(Long id) {
        PlanTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND, "预案模板不存在");
        }
        return template;
    }
}
