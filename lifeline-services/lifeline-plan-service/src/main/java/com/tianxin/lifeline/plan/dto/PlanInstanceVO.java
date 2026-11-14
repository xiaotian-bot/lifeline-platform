package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预案实例视图对象。
 *
 * <p>由 plan_instance 关联 plan_template 回填模板名称与场景类型，
 * 字段命名与下划线列名自动驼峰映射对齐（供 XML resultType 直接映射）。
 */
@Data
public class PlanInstanceVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例ID */
    private Long id;

    /** 实例编号 */
    private String instanceCode;

    /** 预案名称 */
    private String planName;

    /** 关联模板ID */
    private Long templateId;

    /** 模板名称（关联回填） */
    private String templateName;

    /** 场景类型（关联回填） */
    private String scenarioType;

    /** 关联预警事件ID */
    private Long warningEventId;

    /** 实例状态：0草稿/1执行中/2已完成/3已归档/4已取消 */
    private Integer status;

    /** 状态中文 */
    private String statusText;

    /** 实例来源：AI / TEMPLATE */
    private String source;

    /** 生成人ID */
    private Long generatedBy;

    /** 综合处置建议 */
    private String suggestion;

    /** 三级联动上报级别：0 企业运维 / 1 主管部门 / 2 应急联动 */
    private Integer escalationLevel;

    /** 启动时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 任务总数（进度聚合） */
    private Integer taskTotal;

    /** 已完成任务数（进度聚合） */
    private Integer taskCompleted;
}
