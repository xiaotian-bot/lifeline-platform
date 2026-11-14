package com.tianxin.lifeline.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 预案执行实例实体（对应表 plan_instance）。
 *
 * <p>由模板或 AI 生成结果实例化而来；{@code source} / {@code planName} /
 * {@code escalationLevel} 列由增量脚本 08-plan-instance-alter.sql 以幂等方式补齐。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_instance")
public class PlanInstance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例编号（业务可读编码，如 PLAN-2026xxxx-雪花ID） */
    private String instanceCode;

    /** 关联预案模板ID */
    private Long templateId;

    /** 关联预警事件ID（由预警触发时必填） */
    private Long warningEventId;

    /** 实例状态：0 草稿 / 1 执行中 / 2 已完成 / 3 已归档 / 4 已取消 */
    private Integer status;

    /** 生成人ID（当前登录用户） */
    private Long generatedBy;

    /** 预案名称（AI 结果的 planName 或模板名） */
    private String planName;

    /** 实例来源：AI 智能生成 / TEMPLATE 模板实例化 */
    private String source;

    /** 综合处置建议（AI 结果的 suggestion，模板降级时取模板内容） */
    private String suggestion;

    /** 三级联动上报级别：0 企业运维 / 1 主管部门 / 2 应急联动（默认 0） */
    private Integer escalationLevel;

    /** 启动执行时间 */
    private LocalDateTime startTime;

    /** 结束（核销/取消）时间 */
    private LocalDateTime endTime;
}
