package com.tianxin.lifeline.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案任务状态推进请求。
 *
 * <p>目标状态取值：1 执行中 / 2 已完成 / 3 已取消；
 * 状态机合法性由服务层校验（终态任务不允许再流转）。
 */
@Data
public class PlanTaskUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标状态：1 执行中 / 2 已完成 / 3 已取消 */
    @NotNull(message = "任务状态不能为空")
    @Min(value = 1, message = "任务状态取值不合法")
    @Max(value = 3, message = "任务状态取值不合法")
    private Integer status;

    /** 指派人（可空，非空则同步更新） */
    private String assignee;

    /** 备注（写入执行日志） */
    private String remark;
}
