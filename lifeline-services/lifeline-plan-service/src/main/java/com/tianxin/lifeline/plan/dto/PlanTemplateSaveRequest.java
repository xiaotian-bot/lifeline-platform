package com.tianxin.lifeline.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案模板保存请求（创建/更新复用）。
 *
 * <p>创建时全部有效字段落库（雪花 ID 手工填充）；
 * 更新时按非空字段选择性覆盖。
 */
@Data
public class PlanTemplateSaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称长度不能超过128")
    private String templateName;

    /** 模板类型：EMERGENCY 应急处置预案（创建时缺省 EMERGENCY） */
    @Size(max = 32, message = "模板类型长度不能超过32")
    private String templateType;

    /** 场景类型：GAS_LEAK/WATER_BURST/HEATING_LEAK/DRAINAGE_OVERFLOW/
     * ROAD_COLLAPSE/THIRD_PARTY_DAMAGE/CORROSION_FAILURE/COMPREHENSIVE */
    @Size(max = 32, message = "场景类型长度不能超过32")
    private String scenarioType;

    /** 预案内容（处置要点文本） */
    @NotBlank(message = "预案内容不能为空")
    private String content;

    /** 状态：1 启用 / 0 停用（创建时缺省 1 启用） */
    private Integer status;
}
