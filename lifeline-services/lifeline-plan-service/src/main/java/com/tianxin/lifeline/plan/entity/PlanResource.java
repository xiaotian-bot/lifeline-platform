package com.tianxin.lifeline.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 预案资源实体（对应表 plan_resource）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_resource")
public class PlanResource extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属预案实例ID */
    private Long instanceId;

    /** 资源类型：MATERIAL 物资 / EQUIPMENT 设备 / STAFF 人员 */
    private String resourceType;

    /** 资源名称 */
    private String resourceName;

    /** 需求数量 */
    private BigDecimal quantity;

    /** 位置描述 */
    private String location;

    /** 资源状态：0 待调配 / 1 已调配 / 2 已到位 */
    private Integer status;
}
