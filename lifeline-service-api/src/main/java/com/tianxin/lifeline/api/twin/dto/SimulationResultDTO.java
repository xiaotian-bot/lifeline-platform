package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仿真推演查询结果 DTO（服务间共享）。
 */
@Data
public class SimulationResultDTO implements Serializable {

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

    /** 完成时间戳（毫秒） */
    private Long finishTime;

    /** 结构化推演结果（仅 COMPLETED 有值） */
    private SimulationOutcomeDTO outcome;

    /** 失败原因（仅 FAILED 有值） */
    private String error;
}
