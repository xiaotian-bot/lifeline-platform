package com.tianxin.lifeline.warning.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 预警事件状态常量与中文映射。
 *
 * <p>warning_event.status 为 smallint 数值，对外接口统一展示中文：
 * 0 待确认 / 1 已确认 / 2 处置中 / 3 已核销 / 4 误报。
 */
public final class WarningStatus {

    private WarningStatus() {
    }

    /** 待确认 */
    public static final int PENDING = 0;

    /** 已确认 */
    public static final int CONFIRMED = 1;

    /** 处置中 */
    public static final int PROCESSING = 2;

    /** 已核销（已解除） */
    public static final int RESOLVED = 3;

    /** 误报 */
    public static final int FALSE_ALARM = 4;

    /** 数值 → 中文映射（按状态顺序） */
    private static final Map<Integer, String> CODE_TO_TEXT = new LinkedHashMap<>();

    /** 中文 → 数值映射 */
    private static final Map<String, Integer> TEXT_TO_CODE = new LinkedHashMap<>();

    static {
        CODE_TO_TEXT.put(PENDING, "待确认");
        CODE_TO_TEXT.put(CONFIRMED, "已确认");
        CODE_TO_TEXT.put(PROCESSING, "处置中");
        CODE_TO_TEXT.put(RESOLVED, "已核销");
        CODE_TO_TEXT.put(FALSE_ALARM, "误报");
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
     * 获取全部数值 → 中文映射（供统计翻译使用）。
     *
     * @return 映射副本
     */
    public static Map<Integer, String> allText() {
        return new LinkedHashMap<>(CODE_TO_TEXT);
    }
}
