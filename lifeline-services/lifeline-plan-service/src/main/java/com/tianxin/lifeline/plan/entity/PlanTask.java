package com.tianxin.lifeline.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 预案任务实体（对应表 plan_task）。
 *
 * <p>AI 生成结果中的 {@code PlanTaskItemDTO.suggestAssignee} 映射为 {@code assignee}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_task")
public class PlanTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属预案实例ID */
    private Long instanceId;

    /** 任务名称 */
    private String taskName;

    /** 任务类型：如 ISOLATE 隔离关断 / REPAIR 抢修 / MONITOR 监测 / NOTIFY 通知 / GENERAL 通用 */
    private String taskType;

    /** 指派人（运维人员编码或姓名） */
    private String assignee;

    /** 任务状态：0 待执行 / 1 执行中 / 2 已完成 / 3 已取消 */
    private Integer status;

    /** 优先级：1 高 / 2 中 / 3 低 */
    private Integer priority;

    /** 任务开始时间 */
    private LocalDateTime startTime;

    /** 任务完成（或取消）时间 */
    private LocalDateTime endTime;
}
