package com.tianxin.lifeline.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警事件消费负载 DTO（operation-service 本地副本）。
 *
 * <p>字段与 warning-service 发布端 {@code WarningEventPayload} /
 * 共享契约 {@code WarningEventDTO} 对齐，用于反序列化
 * {@code pipe_warning_event} 信封负载，支撑预警→工单自动派单。
 *
 * <p>注意：发布端在预警落库前投递事件，负载不含预警事件主键，
 * 关联追溯以 {@link #warningNo}（预警编号）为准。
 */
@Data
public class WarningEventPayloadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警编号（业务幂等键，格式：设备+指标+时间窗） */
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

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String metricType;

    /** 触发值 */
    private Double triggerValue;

    /** 阈值 */
    private Double threshold;

    /** 命中的规则ID（来源为 RULE 时填充） */
    private Long ruleId;

    /** 预警类型标签（规则名称或异常类型） */
    private String warningType;

    /** 责任人ID */
    private Long assigneeId;

    /** 预警发生时间 */
    private LocalDateTime warningTime;
}
