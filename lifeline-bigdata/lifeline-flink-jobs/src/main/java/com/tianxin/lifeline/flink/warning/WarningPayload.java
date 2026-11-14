package com.tianxin.lifeline.flink.warning;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警事件负载（Flink 侧副本）。
 *
 * <p>字段与 warning-service 的 WarningEventPayload <b>逐字段一致</b>
 * （字段名/类型/JSON 键完全相同），保证 WarningEventListener 的
 * {@code envelope.convertPayload(WarningEventPayload.class)} 可直接反序列化。
 * 两侧独立维护的原因：Flink 作业不依赖 Spring Boot 服务模块，
 * 契约变更时需同步两侧（已在任务验收清单中标注）。
 */
@Data
public class WarningPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警编号（业务幂等键，格式与规则桥一致：W-设备-指标-时间窗） */
    private String warningNo;

    /** 预警来源：RULE / AI / MANUAL */
    private String source;

    /** 预警等级编码：BLUE / YELLOW / ORANGE / RED */
    private String warningLevel;

    /** 管网类型编码 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联设备ID */
    private Long deviceId;

    /** 设备编码（原始字符串形式，如 DEV-GAS-001） */
    private String deviceCode;

    /** 所属区域ID */
    private Long areaId;

    /** 预警标题 */
    private String title;

    /** 预警描述 */
    private String description;

    /** 指标类型（与规则桥口径一致：填充规则 rule_type 语义标签，如 PRESSURE_LOW） */
    private String metricType;

    /** 触发值 */
    private Double triggerValue;

    /** 阈值 */
    private Double threshold;

    /** 命中的规则ID（来源为 RULE 时填充） */
    private Long ruleId;

    /** 预警类型标签（规则名称） */
    private String warningType;

    /** 责任人ID */
    private Long assigneeId;

    /** 预警发生时间 */
    private LocalDateTime warningTime;
}
