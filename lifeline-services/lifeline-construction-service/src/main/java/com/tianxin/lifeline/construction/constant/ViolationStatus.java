package com.tianxin.lifeline.construction.constant;

/**
 * 施工违规记录处理状态常量。
 *
 * <p>违规记录由 AI 视觉识别命中或人工上报产生，产生即触发
 * 区块链存证事件投递；0 待处理 → 1 已处理。
 */
public final class ViolationStatus {

    private ViolationStatus() {
    }

    /** 待处理 */
    public static final int PENDING = 0;

    /** 已处理 */
    public static final int HANDLED = 1;

    /** 违规来源：AI 视觉识别 */
    public static final String SOURCE_VISION = "VISION";

    /** 违规来源：人工上报 */
    public static final String SOURCE_MANUAL = "MANUAL";

    /** AI 降级转人工复核的标记类型（未识别结论） */
    public static final String TYPE_MANUAL_REVIEW = "MANUAL_REVIEW";

    /**
     * 状态数值转中文文本。
     *
     * @param status 状态数值
     * @return 中文文本，未知状态返回"未知"
     */
    public static String toText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case PENDING -> "待处理";
            case HANDLED -> "已处理";
            default -> "未知";
        };
    }
}
