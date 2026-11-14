package com.tianxin.lifeline.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.warning.dto.WarningRuleRequest;
import com.tianxin.lifeline.warning.entity.WarningRule;
import com.tianxin.lifeline.warning.mapper.WarningRuleMapper;
import com.tianxin.lifeline.warning.service.RuleCacheService;
import com.tianxin.lifeline.warning.service.WarningRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 预警规则管理服务实现。
 *
 * <p>规则变更后立即触发规则缓存刷新，缩短判定链路生效延迟。
 */
@Slf4j
@Service
public class WarningRuleServiceImpl implements WarningRuleService {

    /** 规则 Mapper */
    private final WarningRuleMapper warningRuleMapper;

    /** 规则缓存服务 */
    private final RuleCacheService ruleCacheService;

    /**
     * 构造规则管理服务。
     *
     * @param warningRuleMapper 规则 Mapper
     * @param ruleCacheService  规则缓存服务
     */
    public WarningRuleServiceImpl(WarningRuleMapper warningRuleMapper, RuleCacheService ruleCacheService) {
        this.warningRuleMapper = warningRuleMapper;
        this.ruleCacheService = ruleCacheService;
    }

    /**
     * 分页查询预警规则（按更新时间倒序）。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<WarningRule> page(PageQuery query) {
        LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<WarningRule>()
                .orderByDesc(WarningRule::getUpdatedAt);
        Page<WarningRule> page = new Page<>(query.getPage(), query.getSize());
        Page<WarningRule> result = warningRuleMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getSize());
    }

    /**
     * 创建预警规则。
     *
     * @param request 规则请求
     * @return 创建后的规则（含ID）
     */
    @Override
    public WarningRule create(WarningRuleRequest request) {
        WarningRule rule = new WarningRule();
        applyRequest(rule, request);
        if (rule.getStatus() == null) {
            rule.setStatus(WarningRule.STATUS_ENABLED);
        }
        warningRuleMapper.insert(rule);
        ruleCacheService.refresh();
        log.info("预警规则创建成功: id={}, name={}", rule.getId(), rule.getRuleName());
        return rule;
    }

    /**
     * 更新预警规则。
     *
     * @param ruleId  规则ID
     * @param request 规则请求
     * @return 更新后的规则
     */
    @Override
    public WarningRule update(Long ruleId, WarningRuleRequest request) {
        WarningRule rule = warningRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException(ErrorCode.WARNING_RULE_NOT_FOUND);
        }
        applyRequest(rule, request);
        warningRuleMapper.updateById(rule);
        ruleCacheService.refresh();
        log.info("预警规则更新成功: id={}, name={}", ruleId, rule.getRuleName());
        return rule;
    }

    /**
     * 逻辑删除预警规则。
     *
     * @param ruleId 规则ID
     */
    @Override
    public void delete(Long ruleId) {
        WarningRule rule = warningRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException(ErrorCode.WARNING_RULE_NOT_FOUND);
        }
        warningRuleMapper.deleteById(ruleId);
        ruleCacheService.refresh();
        log.info("预警规则删除成功: id={}, name={}", ruleId, rule.getRuleName());
    }

    /**
     * 启用/停用预警规则。
     *
     * @param ruleId 规则ID
     * @param status 目标状态：0 停用 / 1 启用
     * @return 更新后的规则
     */
    @Override
    public WarningRule changeStatus(Long ruleId, Integer status) {
        if (status == null
                || (status != WarningRule.STATUS_ENABLED && status != WarningRule.STATUS_DISABLED)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "规则状态取值必须为 0 或 1");
        }
        WarningRule rule = warningRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException(ErrorCode.WARNING_RULE_NOT_FOUND);
        }
        rule.setStatus(status);
        warningRuleMapper.updateById(rule);
        ruleCacheService.refresh();
        log.info("预警规则状态变更: id={}, status={}", ruleId, status);
        return rule;
    }

    /**
     * 将请求字段应用到实体。
     *
     * @param rule    规则实体
     * @param request 规则请求
     */
    private void applyRequest(WarningRule rule, WarningRuleRequest request) {
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setPipeType(request.getPipeType());
        rule.setConditionExpr(request.getConditionExpr());
        rule.setThresholdValue(request.getThresholdValue());
        rule.setWarningLevel(request.getWarningLevel());
        if (request.getStatus() != null) {
            rule.setStatus(request.getStatus());
        }
    }
}
