package com.tianxin.lifeline.api.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单创建 DTO（服务间共享）。
 */
@Data
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单标题 */
    private String title;

    /** 工单类型：REPAIR 维修 / INSPECT 巡检 / MAINTAIN 养护 / EMERGENCY 应急 */
    private String orderType;

    /** 优先级：1 最高 - 4 最低 */
    private Integer priority;

    /** 管网类型 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联预警事件ID；预警自动派单场景因事件先于落库发布暂以工单描述中的 warningNo 文本关联，可为空 */
    private Long warningId;

    /** 所属区域ID */
    private Long areaId;

    /** 指派责任人ID */
    private Long assigneeId;

    /** 工单描述 */
    private String description;

    /** 创建来源：WARNING / PLAN / MANUAL / AI */
    private String source;
}
