package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预案资源视图对象。
 */
@Data
public class PlanResourceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 资源ID */
    private Long id;

    /** 所属实例ID */
    private Long instanceId;

    /** 资源类型：MATERIAL/EQUIPMENT/STAFF */
    private String resourceType;

    /** 资源名称 */
    private String resourceName;

    /** 需求数量 */
    private BigDecimal quantity;

    /** 位置描述 */
    private String location;

    /** 资源状态：0待调配/1已调配/2已到位 */
    private Integer status;
}
