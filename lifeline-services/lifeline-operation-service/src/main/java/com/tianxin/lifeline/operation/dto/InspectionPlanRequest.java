package com.tianxin.lifeline.operation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 巡检计划新增请求（POST /api/v1/inspections 请求体）。
 *
 * <p>计划创建成功后会自动派生一张 INSPECT 巡检工单（source=PLAN）。
 */
@Data
public class InspectionPlanRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划名称（必填） */
    @NotBlank(message = "计划名称不能为空")
    private String planName;

    /** 计划类型：ROUTINE 定期巡查 / GAS 燃气专项 / DRAINAGE 排水专项等 */
    private String planType;

    /** 所属区域编码 */
    private String areaId;

    /** 巡检频次：DAILY 每日 / WEEKLY 每周 / MONTHLY 每月 */
    private String frequency;
}
