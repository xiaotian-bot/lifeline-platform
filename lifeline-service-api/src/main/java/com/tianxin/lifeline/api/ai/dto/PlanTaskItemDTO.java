package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI预案任务条目 DTO（服务间共享）。
 *
 * <p>预案生成结果中的单条处置任务，供调用方落库为 plan_task。
 */
@Data
public class PlanTaskItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 任务名称 */
    private String taskName;

    /** 任务类型：如 ISOLATE 隔离关断 / REPAIR 抢修 / MONITOR 监测 / NOTIFY 通知 */
    private String taskType;

    /** 优先级：1高/2中/3低 */
    private Integer priority;

    /** 建议指派人（运维人员编码或姓名） */
    private String suggestAssignee;
}
