package com.tianxin.lifeline.flink.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预警规则模型（对应 PostgreSQL warning_rule 表启用记录）。
 *
 * <p>字段与 01-postgres-init.sql 的 warning_rule 表结构对齐：
 * rule_type 为指标语义标签（如 PRESSURE_LOW / FLOW_HIGH），
 * condition_expr 为条件表达式文本（如 {@code pressure < threshold}），
 * 实际取值指标与比较符均以 condition_expr 解析结果为准
 * （与 07-seed-p9.sql 规则种子的文本约定一致）。
 */
@Data
public class WarningRuleModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型（指标语义标签：PRESSURE_LOW / FLOW_HIGH 等） */
    private String ruleType;

    /** 管网类型限定（空表示不限；gas/water 等，大小写不敏感匹配） */
    private String pipeType;

    /** 条件表达式：{指标} {比较符} threshold，如 pressure < threshold */
    private String conditionExpr;

    /** 阈值 */
    private Double thresholdValue;

    /** 预警级别：1 红 / 2 橙 / 3 黄 / 4 蓝（数值越小风险越高） */
    private Integer warningLevel;
}
