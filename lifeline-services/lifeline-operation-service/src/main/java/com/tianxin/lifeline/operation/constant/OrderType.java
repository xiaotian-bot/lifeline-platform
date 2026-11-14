package com.tianxin.lifeline.operation.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维工单类型常量与中文映射。
 *
 * <p>operation_order.order_type 为英文编码，对外接口附中文展示：
 * REPAIR 维修 / INSPECT 巡检 / MAINTAIN 养护 / EMERGENCY 应急。
 */
public final class OrderType {

    private OrderType() {
    }

    /** 维修 */
    public static final String REPAIR = "REPAIR";

    /** 巡检 */
    public static final String INSPECT = "INSPECT";

    /** 养护 */
    public static final String MAINTAIN = "MAINTAIN";

    /** 应急 */
    public static final String EMERGENCY = "EMERGENCY";

    /** 编码 → 中文映射 */
    private static final Map<String, String> CODE_TO_TEXT = new LinkedHashMap<>();

    static {
        CODE_TO_TEXT.put(REPAIR, "维修");
        CODE_TO_TEXT.put(INSPECT, "巡检");
        CODE_TO_TEXT.put(MAINTAIN, "养护");
        CODE_TO_TEXT.put(EMERGENCY, "应急");
    }

    /**
     * 类型编码转中文。
     *
     * @param code 类型编码
     * @return 中文类型，未知返回原编码
     */
    public static String toText(String code) {
        return code == null ? "" : CODE_TO_TEXT.getOrDefault(code, code);
    }

    /**
     * 判断类型编码是否合法。
     *
     * @param code 类型编码
     * @return true 表示合法
     */
    public static boolean isValid(String code) {
        return code != null && CODE_TO_TEXT.containsKey(code);
    }

    /**
     * 获取全部编码 → 中文映射。
     *
     * @return 映射副本
     */
    public static Map<String, String> allText() {
        return new LinkedHashMap<>(CODE_TO_TEXT);
    }
}
