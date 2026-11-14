package com.tianxin.lifeline.plan.service;

import com.tianxin.lifeline.plan.dto.PlanGenerateRequest;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;

/**
 * 预案生成业务服务。
 *
 * <p>优先经 AI 预案代理智能生成（source=AI）；Feign 调用失败或
 * 返回无效时降级为按模板 content 实例化通用预案（source=TEMPLATE），
 * 接口永不失败。生成结果落库 plan_instance / plan_task / plan_resource，
 * 并写 plan_log 留痕。
 */
public interface PlanGenerateService {

    /**
     * 生成处置预案实例。
     *
     * @param request 预案生成请求
     * @return 生成成功的预案实例视图
     */
    PlanInstanceVO generate(PlanGenerateRequest request);
}
