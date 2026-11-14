package com.tianxin.lifeline.operation.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 运维工单分页查询请求。
 *
 * <p>支持状态 / 工单类型 / 预警事件ID / 责任人 / 优先级 / 创建时间范围组合过滤。
 * status 兼容中文（待派单/待接单/处置中/已完成/已关闭）与数值（0-4）。
 * 默认排序：红色预警应急工单（source=WARNING 且 priority=1）置顶，
 * 其次按优先级升序、创建时间降序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单状态：中文（待派单/待接单/处置中/已完成/已关闭）或数值（0-4） */
    private String status;

    /** 工单类型：REPAIR / INSPECT / MAINTAIN / EMERGENCY */
    private String orderType;

    /** 关联预警事件ID */
    private Long warningEventId;

    /** 责任人标识（指派的责任人ID） */
    private String assignee;

    /** 优先级：1 最高 - 4 最低 */
    private Integer priority;

    /** 创建时间起（格式 yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 创建时间止（格式 yyyy-MM-dd HH:mm:ss） */
    private String endTime;
}
