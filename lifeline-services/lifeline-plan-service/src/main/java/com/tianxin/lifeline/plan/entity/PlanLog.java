package com.tianxin.lifeline.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 预案执行日志实体（对应表 plan_log）。
 *
 * <p>预案执行全流程（生成/启动/任务推进/核销/取消/处置反馈回写）留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_log")
public class PlanLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属预案实例ID */
    private Long instanceId;

    /** 动作编码：GENERATE 生成 / START 启动 / TASK_UPDATE 任务推进 /
     * COMPLETE 核销 / CANCEL 取消 / DISPOSAL_WRITEBACK 处置回写 */
    private String action;

    /** 操作人（用户名或系统标识） */
    private String operator;

    /** 日志内容描述 */
    private String content;

    /** 动作发生时间 */
    private LocalDateTime logTime;
}
