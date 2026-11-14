package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仿真推演查询结果（GET /simulation/{taskId}）。
 *
 * <p>轮询用：状态 RUNNING 时 outcome 为空；COMPLETED 时携带结构化结果；
 * FAILED 时携带 error 说明（接口层面仍返回 200，由 status 区分）。
 */
@Data
public class SimulationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仿真任务ID */
    private String taskId;

    /** 任务状态：RUNNING / COMPLETED / FAILED */
    private String status;

    /** 场景类型 */
    private String scenarioType;

    /** 提交时间戳（毫秒） */
    private Long submitTime;

    /** 完成时间戳（毫秒，运行中为 null） */
    private Long finishTime;

    /** 结构化推演结果（仅 COMPLETED 有值） */
    private SimulationOutcome outcome;

    /** 失败原因（仅 FAILED 有值） */
    private String error;
}
