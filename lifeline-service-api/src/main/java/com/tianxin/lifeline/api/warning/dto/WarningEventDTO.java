package com.tianxin.lifeline.api.warning.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警事件 DTO（服务间共享）。
 */
@Data
public class WarningEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件ID */
    private Long warningId;

    /** 预警事件编号 */
    private String warningNo;

    /** 预警等级：BLUE/YELLOW/ORANGE/RED */
    private String warningLevel;

    /** 预警来源：RULE 规则引擎 / AI 模型 / MANUAL 人工 */
    private String source;

    /** 管网类型 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联设备ID */
    private Long deviceId;

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

    /** 预警状态：待确认/已确认/处置中/已核销 */
    private String status;

    /** 预警发生时间 */
    private LocalDateTime warningTime;

    /** 责任人ID */
    private Long assigneeId;
}
