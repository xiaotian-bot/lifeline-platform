package com.tianxin.lifeline.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运维工单视图对象（对外输出，状态与类型附中文）。
 */
@Data
public class OrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long id;

    /** 工单编号 */
    private String orderCode;

    /** 工单标题 */
    private String title;

    /** 工单类型编码：REPAIR / INSPECT / MAINTAIN / EMERGENCY */
    private String orderType;

    /** 工单类型中文：维修 / 巡检 / 养护 / 应急 */
    private String orderTypeText;

    /** 工单状态数值：0-4 */
    private Integer status;

    /** 工单状态中文：待派单/待接单/处置中/已完成/已关闭 */
    private String statusText;

    /** 优先级：1 最高 - 4 最低 */
    private Integer priority;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联预警事件ID */
    private Long warningEventId;

    /** 责任人标识 */
    private String assignee;

    /** 所属区域ID */
    private Long areaId;

    /** 工单来源：WARNING / PLAN / MANUAL / AI */
    private String source;

    /** 工单内容描述 */
    private String content;

    /** 处置结果 */
    private String result;

    /** 处置开始时间 */
    private LocalDateTime startTime;

    /** 处置结束时间 */
    private LocalDateTime endTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
