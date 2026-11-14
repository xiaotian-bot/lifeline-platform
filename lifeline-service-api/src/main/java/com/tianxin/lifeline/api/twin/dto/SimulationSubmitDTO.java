package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仿真推演提交响应 DTO（服务间共享）。
 */
@Data
public class SimulationSubmitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仿真任务ID */
    private String taskId;

    /** 任务状态 */
    private String status;

    /** 提交时间戳（毫秒） */
    private Long submitTime;
}
