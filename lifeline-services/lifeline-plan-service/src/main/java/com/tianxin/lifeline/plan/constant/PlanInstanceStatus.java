package com.tianxin.lifeline.plan.constant;

/**
 * 预案实例状态机常量。
 *
 * <p>状态流转：0 草稿 → 1 执行中 → 2 已完成 → 3 已归档；
 * 0/1 状态可取消（→ 4 已取消）。非法流转由服务层校验并抛业务异常。
 */
public final class PlanInstanceStatus {

    private PlanInstanceStatus() {
    }

    /** 草稿（生成后待启动） */
    public static final int DRAFT = 0;

    /** 执行中 */
    public static final int RUNNING = 1;

    /** 已完成（全部任务完成核销） */
    public static final int COMPLETED = 2;

    /** 已归档 */
    public static final int ARCHIVED = 3;

    /** 已取消 */
    public static final int CANCELLED = 4;

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
            case DRAFT -> "草稿";
            case RUNNING -> "执行中";
            case COMPLETED -> "已完成";
            case ARCHIVED -> "已归档";
            case CANCELLED -> "已取消";
            default -> "";
        };
    }

    /**
     * 判断是否为终态（已完成/已归档/已取消），终态不允许再流转。
     *
     * @param status 状态码
     * @return true 表示终态
     */
    public static boolean isTerminal(Integer status) {
        return status != null && (status == COMPLETED || status == ARCHIVED || status == CANCELLED);
    }
}
