package com.tianxin.lifeline.twin.constant;

/**
 * 仿真推演任务状态常量。
 *
 * <p>任务状态由 Redis HASH 承载（TTL 30 分钟），不落 PG。
 */
public final class SimulationStatus {

    private SimulationStatus() {
    }

    /** 运行中（已提交，线程池执行推演中） */
    public static final String RUNNING = "RUNNING";

    /** 已完成（推演结果可查） */
    public static final String COMPLETED = "COMPLETED";

    /** 失败（推演过程异常，接口层面仍返回 200 + 状态标记） */
    public static final String FAILED = "FAILED";

    /* ==================== Redis HASH 字段名 ==================== */

    /** 任务ID字段 */
    public static final String FIELD_TASK_ID = "taskId";

    /** 任务状态字段 */
    public static final String FIELD_STATUS = "status";

    /** 场景类型字段 */
    public static final String FIELD_SCENARIO = "scenario";

    /** 提交时间戳（毫秒）字段 */
    public static final String FIELD_SUBMIT_TIME = "submitTime";

    /** 完成时间戳（毫秒）字段 */
    public static final String FIELD_FINISH_TIME = "finishTime";

    /** 推演结果 JSON 字段 */
    public static final String FIELD_RESULT = "result";

    /** 失败原因字段 */
    public static final String FIELD_ERROR = "error";
}
