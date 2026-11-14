package com.tianxin.lifeline.construction.constant;

/**
 * 施工工序质检状态常量。
 *
 * <p>0 待质检 → 1 质检中（人工/AI 视觉质检进行中或转人工复核）
 * → 2 质检通过 / 3 质检不通过（命中违规）。
 */
public final class ProcessQcStatus {

    private ProcessQcStatus() {
    }

    /** 待质检 */
    public static final int PENDING = 0;

    /** 质检中（含 AI 降级转人工复核场景） */
    public static final int INSPECTING = 1;

    /** 质检通过 */
    public static final int PASSED = 2;

    /** 质检不通过（命中违规） */
    public static final int REJECTED = 3;

    /** 质检合格分数线（百分制） */
    public static final int PASS_LINE = 60;

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
            case PENDING -> "待质检";
            case INSPECTING -> "质检中";
            case PASSED -> "质检通过";
            case REJECTED -> "质检不通过";
            default -> "未知";
        };
    }
}
