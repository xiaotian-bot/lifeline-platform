package com.tianxin.lifeline.warning.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预警事件分页查询请求。
 *
 * <p>支持等级 / 状态 / 管网类型 / 管网ID / 设备ID / 时间范围组合过滤。
 * level 兼容等级编码（RED/ORANGE/YELLOW/BLUE）与数值（1-4）；
 * status 兼容中文（待确认/已确认/处置中/已核销/误报）与数值（0-4）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WarningQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警等级：RED/ORANGE/YELLOW/BLUE 或 1/2/3/4 */
    private String level;

    /** 预警状态：中文（待确认/已确认/处置中/已核销）或数值（0-4） */
    private String status;

    /** 管网类型编码 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联设备ID */
    private Long deviceId;

    /** 预警发生时间起（格式 yyyy-MM-dd HH:mm:ss） */
    private String startTime;

    /** 预警发生时间止（格式 yyyy-MM-dd HH:mm:ss） */
    private String endTime;
}
