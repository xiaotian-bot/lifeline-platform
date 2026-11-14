package com.tianxin.lifeline.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 巡检计划实体（对应表 inspection_plan）。
 *
 * <p>计划创建成功后会自动派生一张 INSPECT 类型的巡检工单
 * （source=PLAN，content 携带 [inspectionPlanId=计划ID] 关联标记）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_plan")
public class InspectionPlan extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划名称 */
    private String planName;

    /** 计划类型：ROUTINE 定期巡查 / GAS 燃气专项 / DRAINAGE 排水专项等 */
    private String planType;

    /** 所属区域编码 */
    private String areaId;

    /** 巡检频次：DAILY 每日 / WEEKLY 每周 / MONTHLY 每月 */
    private String frequency;

    /** 计划状态：1 启用 / 0 停用 */
    private Integer status;
}
