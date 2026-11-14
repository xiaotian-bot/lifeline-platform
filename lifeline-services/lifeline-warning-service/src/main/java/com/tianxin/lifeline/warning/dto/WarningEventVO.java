package com.tianxin.lifeline.warning.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警事件视图对象（分页列表与统计展示用）。
 *
 * <p>相对库表结构做了展示层翻译：warningLevel 输出等级编码，
 * status 输出中文（待确认/已确认/处置中/已核销/误报）。
 */
@Data
public class WarningEventVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件ID */
    private Long warningId;

    /** 预警事件编号 */
    private String warningNo;

    /** 预警等级编码：BLUE / YELLOW / ORANGE / RED */
    private String warningLevel;

    /** 预警等级名称：蓝色预警 / 黄色预警 / 橙色预警 / 红色预警 */
    private String levelName;

    /** 等级展示色值 */
    private String levelColor;

    /** 预警来源：RULE / AI / MANUAL */
    private String source;

    /** 管网类型编码 */
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

    /** 指标类型 */
    private String metricType;

    /** 触发值 */
    private BigDecimal triggerValue;

    /** 阈值 */
    private BigDecimal threshold;

    /** AI 异常评分（0~1，纯规则判定为 null） */
    private BigDecimal aiScore;

    /** AI 研判结论摘要（未研判为 null） */
    private String aiResult;

    /** AI 研判完成时间（未研判为 null） */
    private LocalDateTime aiJudgedAt;

    /** 状态中文：待确认 / 已确认 / 处置中 / 已核销 / 误报 */
    private String status;

    /** 预警发生时间（创建时间） */
    private LocalDateTime warningTime;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 核销时间 */
    private LocalDateTime resolvedAt;

    /** 责任人ID */
    private Long assigneeId;
}
