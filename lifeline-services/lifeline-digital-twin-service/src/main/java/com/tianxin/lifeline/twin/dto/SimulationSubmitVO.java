package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仿真推演提交响应（POST /simulation）。
 *
 * <p>立即返回任务ID与初始状态，不阻塞等待推演完成。
 */
@Data
public class SimulationSubmitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 仿真任务ID（雪花ID字符串） */
    private String taskId;

    /** 任务状态（提交后固定为 RUNNING） */
    private String status;

    /** 提交时间戳（毫秒） */
    private Long submitTime;
}
