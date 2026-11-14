package com.tianxin.lifeline.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 运维工单实体（对应表 operation_order）。
 *
 * <p>状态机：0 待派单 → 1 待接单 → 2 处置中 → 3 已完成，0/1/2 可关闭 → 4 已关闭。
 * 幂等约束：order_code 上存在部分唯一索引 uk_operation_order_code（deleted=0）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_order")
public class OperationOrder extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单编号（业务唯一键，对应唯一索引 uk_operation_order_code） */
    private String orderCode;

    /** 工单标题 */
    private String title;

    /** 工单类型：REPAIR 维修 / INSPECT 巡检 / MAINTAIN 养护 / EMERGENCY 应急 */
    private String orderType;

    /** 关联预警事件ID（由预警触发时非空） */
    private Long warningEventId;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联管网ID */
    private Long pipeId;

    /** 责任人标识（指派的责任人ID字符串，未派单为空） */
    private String assignee;

    /** 工单状态：0 待派单 / 1 待接单 / 2 处置中 / 3 已完成 / 4 已关闭 */
    private Integer status;

    /** 优先级：1 最高 - 4 最低 */
    private Integer priority;

    /** 工单内容描述 */
    private String content;

    /** 处置结果（完成时填写） */
    private String result;

    /** 处置开始时间（接单时写入） */
    private LocalDateTime startTime;

    /** 处置结束时间（完成/关闭时写入） */
    private LocalDateTime endTime;

    /** 所属区域ID */
    private Long areaId;

    /** 工单来源：WARNING 预警转派 / PLAN 预案派生 / MANUAL 人工创建 / AI 智能建议 */
    private String source;
}
