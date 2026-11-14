package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI预案资源条目 DTO（服务间共享）。
 *
 * <p>预案生成结果中的单条应急资源需求，供调用方落库为 plan_resource。
 */
@Data
public class PlanResourceItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 资源类型：如 MATERIAL 物资 / EQUIPMENT 设备 / STAFF 人员 */
    private String resourceType;

    /** 资源名称 */
    private String resourceName;

    /** 需求数量 */
    private Double quantity;
}
