package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.warning.dto.WarningRuleRequest;
import com.tianxin.lifeline.warning.entity.WarningRule;

/**
 * 预警规则管理服务。
 *
 * <p>提供规则分页查询与增删改（含启用/停用）。规则变更由
 * RuleCacheService 定时刷新后对判定链路生效。
 */
public interface WarningRuleService {

    /**
     * 分页查询预警规则。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<WarningRule> page(PageQuery query);

    /**
     * 创建预警规则。
     *
     * @param request 规则请求
     * @return 创建后的规则（含ID）
     */
    WarningRule create(WarningRuleRequest request);

    /**
     * 更新预警规则。
     *
     * @param ruleId  规则ID
     * @param request 规则请求
     * @return 更新后的规则
     */
    WarningRule update(Long ruleId, WarningRuleRequest request);

    /**
     * 逻辑删除预警规则。
     *
     * @param ruleId 规则ID
     */
    void delete(Long ruleId);

    /**
     * 启用/停用预警规则。
     *
     * @param ruleId 规则ID
     * @param status 目标状态：0 停用 / 1 启用
     * @return 更新后的规则
     */
    WarningRule changeStatus(Long ruleId, Integer status);
}
