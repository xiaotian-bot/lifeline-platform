package com.tianxin.lifeline.construction.constant;

/**
 * 施工项目状态常量与状态机流转规则。
 *
 * <p>状态机：0 待开工 →(开工)→ 1 施工中 →(完工)→ 2 已完工；
 * 施工中可停工 → 3 已停工，已停工可复工 → 1 施工中；已完工为终态。
 */
public final class ProjectStatus {

    private ProjectStatus() {
    }

    /** 待开工 */
    public static final int PENDING_START = 0;

    /** 施工中 */
    public static final int IN_PROGRESS = 1;

    /** 已完工（终态） */
    public static final int COMPLETED = 2;

    /** 已停工 */
    public static final int SUSPENDED = 3;

    /**
     * 状态机流转校验：是否允许从当前状态流转至目标状态。
     *
     * @param current 当前状态
     * @param target  目标状态
     * @return true 允许流转
     */
    public static boolean canTransit(Integer current, Integer target) {
        if (current == null || target == null || current.equals(target)) {
            return false;
        }
        return (current == PENDING_START && target == IN_PROGRESS)
                || (current == IN_PROGRESS && target == COMPLETED)
                || (current == IN_PROGRESS && target == SUSPENDED)
                || (current == SUSPENDED && target == IN_PROGRESS);
    }

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
            case PENDING_START -> "待开工";
            case IN_PROGRESS -> "施工中";
            case COMPLETED -> "已完工";
            case SUSPENDED -> "已停工";
            default -> "未知";
        };
    }
}
