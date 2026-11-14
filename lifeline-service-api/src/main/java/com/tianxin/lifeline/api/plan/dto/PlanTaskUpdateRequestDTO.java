package com.tianxin.lifeline.api.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案任务状态推进请求 DTO（服务间共享）。
 */
@Data
public class PlanTaskUpdateRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标状态：1 执行中 / 2 已完成 / 3 已取消 */
    private Integer status;

    /** 指派人 */
    private String assignee;

    /** 备注 */
    private String remark;
}
