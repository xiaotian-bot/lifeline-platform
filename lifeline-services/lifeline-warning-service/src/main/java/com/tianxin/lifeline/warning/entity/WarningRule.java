package com.tianxin.lifeline.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 预警规则实体（对应表 warning_rule）。
 *
 * <p>规则判定桥的判定依据：rule_type 表示监控的指标类型
 * （pressure/temperature/flow/vibration/corrosion/leakage），
 * pipe_type 限定管网类型（为空表示不限），threshold_value 为越限阈值，
 * warning_level 为命中后产生的预警级别（1-4）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_rule")
public class WarningRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则状态：启用 */
    public static final int STATUS_ENABLED = 1;

    /** 规则状态：停用 */
    public static final int STATUS_DISABLED = 0;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型（监控指标）：pressure/temperature/flow/vibration/corrosion/leakage */
    private String ruleType;

    /** 适用管网类型（为空表示全部管网）：GAS / WATER / SEWAGE / HEAT / HAZARD */
    private String pipeType;

    /** 条件表达式（预留扩展，当前默认语义为「指标值 >= 阈值」） */
    private String conditionExpr;

    /** 越限阈值 */
    private BigDecimal thresholdValue;

    /** 命中后产生的预警级别数值（1 红最高 / 2 橙 / 3 黄 / 4 蓝最低） */
    private Integer warningLevel;

    /** 规则状态：0 停用 / 1 启用 */
    private Integer status;
}
