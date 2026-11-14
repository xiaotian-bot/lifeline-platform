package com.tianxin.lifeline.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警事件实体（对应表 warning_event）。
 *
 * <p>状态机：0 待确认 → 1 已确认 → 2 处置中 → 3 已核销（已解除），4 误报。
 * 幂等约束：event_code 上存在部分唯一索引 uk_warning_event_code（deleted=0），
 * 作为 Redis 幂等去重之后的数据库层兜底。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_event")
public class WarningEvent extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件编号（业务幂等键，对应唯一索引 uk_warning_event_code） */
    private String eventCode;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联管网ID */
    private Long pipeId;

    /** 管网类型编码：GAS / WATER / SEWAGE / HEAT / HAZARD */
    private String pipeType;

    /** 预警级别数值（1 红最高 / 2 橙 / 3 黄 / 4 蓝最低） */
    private Integer warningLevel;

    /** 预警类型（规则名/异常类型等分类标签） */
    private String warningType;

    /** 预警描述 */
    private String description;

    /** AI 异常评分（0~1，AI 深研结果；纯规则判定为 null） */
    private BigDecimal aiScore;

    /** AI 研判结论摘要（根因/等级复核/影响范围/趋势/处置优先级），未研判为 null */
    private String aiResult;

    /** AI 研判完成时间（非 null 表示已完成 AI 研判，用于去重） */
    private LocalDateTime aiJudgedAt;

    /** 预警状态：0 待确认 / 1 已确认 / 2 处置中 / 3 已核销 / 4 误报 */
    private Integer status;

    /** 确认人ID */
    private Long confirmedBy;

    /** 确认时间 */
    private LocalDateTime confirmedAt;

    /** 核销（解除）时间 */
    private LocalDateTime resolvedAt;

    /** 预警来源：RULE 规则引擎 / AI 模型 / MANUAL 人工 */
    private String source;

    /** 所属区域ID */
    private Long areaId;

    /** 预警标题 */
    private String title;

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String metricType;

    /** 触发值（命中规则时的实际监测值） */
    private BigDecimal triggerValue;

    /** 阈值（命中规则的阈值） */
    private BigDecimal threshold;

    /** 责任人ID（处置指派） */
    private Long assigneeId;
}
