package com.tianxin.lifeline.operation.constant;

import java.util.Set;

/**
 * 运维工单来源常量。
 *
 * <p>operation_order.source 标识工单创建渠道：
 * WARNING 预警转派 / PLAN 预案派生 / MANUAL 人工创建 / AI 智能建议。
 */
public final class OrderSource {

    private OrderSource() {
    }

    /** 预警转派 */
    public static final String WARNING = "WARNING";

    /** 预案派生 */
    public static final String PLAN = "PLAN";

    /** 人工创建 */
    public static final String MANUAL = "MANUAL";

    /** AI 智能建议 */
    public static final String AI = "AI";

    /** 全部合法来源集合 */
    private static final Set<String> ALL = Set.of(WARNING, PLAN, MANUAL, AI);

    /**
     * 判断来源编码是否合法。
     *
     * @param code 来源编码
     * @return true 表示合法
     */
    public static boolean isValid(String code) {
        return code != null && ALL.contains(code);
    }
}
