package com.tianxin.lifeline.api.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预案模板保存请求 DTO（服务间共享，创建/更新复用）。
 */
@Data
public class PlanTemplateSaveRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 模板名称 */
    private String templateName;

    /** 模板类型：EMERGENCY */
    private String templateType;

    /** 场景类型 */
    private String scenarioType;

    /** 预案内容 */
    private String content;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;
}
