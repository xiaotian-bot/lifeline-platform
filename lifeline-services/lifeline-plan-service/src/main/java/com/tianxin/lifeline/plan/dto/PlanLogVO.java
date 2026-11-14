package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预案执行日志视图对象。
 */
@Data
public class PlanLogVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long id;

    /** 所属实例ID */
    private Long instanceId;

    /** 动作编码 */
    private String action;

    /** 操作人 */
    private String operator;

    /** 日志内容 */
    private String content;

    /** 动作发生时间 */
    private LocalDateTime logTime;
}
