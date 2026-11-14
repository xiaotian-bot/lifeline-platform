package com.tianxin.lifeline.warning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 预警规则创建/更新请求。
 */
@Data
public class WarningRuleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则名称 */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /** 规则类型（监控指标）：pressure/temperature/flow/vibration/corrosion/leakage */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /** 适用管网类型（为空表示全部管网） */
    private String pipeType;

    /** 条件表达式（预留扩展） */
    private String conditionExpr;

    /** 越限阈值 */
    @NotNull(message = "阈值不能为空")
    private BigDecimal thresholdValue;

    /** 命中后产生的预警级别数值（1 红最高 / 4 蓝最低） */
    @NotNull(message = "预警级别不能为空")
    @Min(value = 1, message = "预警级别取值 1-4")
    @Max(value = 4, message = "预警级别取值 1-4")
    private Integer warningLevel;

    /** 规则状态：0 停用 / 1 启用，缺省启用 */
    private Integer status;
}
