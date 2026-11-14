package com.tianxin.lifeline.plan.constant;

/**
 * 预案实例来源常量。
 */
public final class PlanSource {

    private PlanSource() {
    }

    /** AI 智能生成（ai-gateway-service 预案代理产出） */
    public static final String AI = "AI";

    /** 模板实例化（AI 降级或指定模板直接实例化） */
    public static final String TEMPLATE = "TEMPLATE";
}
