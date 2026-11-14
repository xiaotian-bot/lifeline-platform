package com.tianxin.lifeline.plan.constant;

/**
 * 预案三级联动上报级别常量（对应 plan_instance.escalation_level，
 * 由增量脚本 08-plan-instance-alter.sql 以幂等方式补齐）。
 *
 * <p>级别只能逐级 +1，不允许跳级或降级。政府侧外部系统对接
 * 当前仅以 plan_log（action=ESCALATE）与事件位留痕，不做真实集成。
 */
public final class PlanEscalationLevel {

    private PlanEscalationLevel() {
    }

    /** 企业运维（默认级别） */
    public static final int ENTERPRISE = 0;

    /** 主管部门 */
    public static final int AUTHORITY = 1;

    /** 应急联动（上限级别） */
    public static final int EMERGENCY_LINKAGE = 2;

    /** 级别上限 */
    public static final int MAX_LEVEL = EMERGENCY_LINKAGE;

    /**
     * 级别码转中文文本。
     *
     * @param level 级别码
     * @return 中文文本，未知级别返回空串
     */
    public static String toText(Integer level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case ENTERPRISE -> "企业运维";
            case AUTHORITY -> "主管部门";
            case EMERGENCY_LINKAGE -> "应急联动";
            default -> "";
        };
    }
}
