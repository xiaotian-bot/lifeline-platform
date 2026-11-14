package com.tianxin.lifeline.api.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预案实例详情聚合 DTO（服务间共享）。
 */
@Data
public class PlanDetailDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例主体信息 */
    private PlanInstanceDTO instance;

    /** 任务清单 */
    private List<PlanTaskDTO> tasks;
}
