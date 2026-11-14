package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预案任务视图对象。
 */
@Data
public class PlanTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long id;

    /** 所属实例ID */
    private Long instanceId;

    /** 任务名称 */
    private String taskName;

    /** 任务类型 */
    private String taskType;

    /** 指派人 */
    private String assignee;

    /** 任务状态：0待执行/1执行中/2已完成/3已取消 */
    private Integer status;

    /** 状态中文 */
    private String statusText;

    /** 优先级：1高/2中/3低 */
    private Integer priority;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
