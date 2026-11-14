package com.tianxin.lifeline.operation.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维工单状态常量与中文映射。
 *
 * <p>operation_order.status 为 smallint 数值，对外接口统一展示中文：
 * 0 待派单 / 1 待接单 / 2 处置中 / 3 已完成 / 4 已关闭。
 *
 * <p>状态机流转规则：
 * <ul>
 *     <li>创建：未指派责任人 → 0 待派单；已指派 → 1 待接单</li>
 *     <li>接单 accept：1 → 2（写 start_time）</li>
 *     <li>完成 complete：2 → 3（写 result / end_time）</li>
 *     <li>关闭 close：0 / 1 / 2 → 4（写 end_time）</li>
 * </ul>
 */
public final class OrderStatus {

    private OrderStatus() {
    }

    /** 待派单 */
    public static final int PENDING_DISPATCH = 0;

    /** 待接单 */
    public static final int PENDING_ACCEPT = 1;

    /** 处置中 */
    public static final int PROCESSING = 2;

    /** 已完成 */
    public static final int COMPLETED = 3;

    /** 已关闭 */
    public static final int CLOSED = 4;

    /** 数值 → 中文映射（按状态顺序） */
    private static final Map<Integer, String> CODE_TO_TEXT = new LinkedHashMap<>();

    /** 中文 → 数值映射 */
    private static final Map<String, Integer> TEXT_TO_CODE = new LinkedHashMap<>();

    static {
        CODE_TO_TEXT.put(PENDING_DISPATCH, "待派单");
        CODE_TO_TEXT.put(PENDING_ACCEPT, "待接单");
        CODE_TO_TEXT.put(PROCESSING, "处置中");
        CODE_TO_TEXT.put(COMPLETED, "已完成");
        CODE_TO_TEXT.put(CLOSED, "已关闭");
        CODE_TO_TEXT.forEach((code, text) -> TEXT_TO_CODE.put(text, code));
    }

    /**
     * 数值状态转中文。
     *
     * @param code 状态数值
     * @return 中文状态，未知返回空串
     */
    public static String toText(Integer code) {
        return code == null ? "" : CODE_TO_TEXT.getOrDefault(code, "");
    }

    /**
     * 中文状态转数值。
     *
     * @param text 中文状态
     * @return 状态数值，未知返回 null
     */
    public static Integer fromText(String text) {
        return text == null ? null : TEXT_TO_CODE.get(text.trim());
    }

    /**
     * 判断状态是否为终态（已完成 / 已关闭，不允许再流转）。
     *
     * @param code 状态数值
     * @return true 表示终态
     */
    public static boolean isTerminal(Integer code) {
        return code != null && (code == COMPLETED || code == CLOSED);
    }

    /**
     * 获取全部数值 → 中文映射（供统计翻译使用）。
     *
     * @return 映射副本
     */
    public static Map<Integer, String> allText() {
        return new LinkedHashMap<>(CODE_TO_TEXT);
    }
}
