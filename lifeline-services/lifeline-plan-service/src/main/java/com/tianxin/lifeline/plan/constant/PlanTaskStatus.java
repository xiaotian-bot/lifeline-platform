package com.tianxin.lifeline.plan.constant;

/**
 * 预案任务状态常量。
 *
 * <p>状态流转：0 待执行 → 1 执行中 → 2 已完成；0/1 可取消（→ 3 已取消）。
 */
public final class PlanTaskStatus {

    private PlanTaskStatus() {
    }

    /** 待执行 */
    public static final int PENDING = 0;

    /** 执行中 */
    public static final int PROCESSING = 1;

    /** 已完成 */
    public static final int COMPLETED = 2;

    /** 已取消 */
    public static final int CANCELLED = 3;

    /**
     * 状态码转中文文本。
     *
     * @param status 状态码
     * @return 中文文本，未知状态返回空串
     */
    public static String toText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "待执行";
            case PROCESSING -> "执行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            default -> "";
        };
    }

    /**
     * 判断是否为终态（已完成/已取消）。
     *
     * @param status 状态码
     * @return true 表示终态
     */
    public static boolean isTerminal(Integer status) {
        return status != null && (status == COMPLETED || status == CANCELLED);
    }
}
