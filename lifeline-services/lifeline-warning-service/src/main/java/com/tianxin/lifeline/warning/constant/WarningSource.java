package com.tianxin.lifeline.warning.constant;

/**
 * 预警来源常量。
 *
 * <p>warning_event.source 取值，标识预警由何种途径产生。
 */
public final class WarningSource {

    private WarningSource() {
    }

    /** 规则引擎判定（传感数据越限） */
    public static final String RULE = "RULE";

    /** AI 模型深研产出 */
    public static final String AI = "AI";

    /** 人工手动创建 */
    public static final String MANUAL = "MANUAL";

    /**
     * 校验来源取值是否合法，非法时归一为 MANUAL。
     *
     * @param source 来源字符串
     * @return 归一化后的来源
     */
    public static String normalize(String source) {
        if (RULE.equalsIgnoreCase(source) || AI.equalsIgnoreCase(source) || MANUAL.equalsIgnoreCase(source)) {
            return source.toUpperCase();
        }
        return MANUAL;
    }
}
