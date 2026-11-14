package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单条预警定位 DTO（服务间共享）。
 */
@Data
public class WarningLocationItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件ID */
    private Long warningId;

    /** 预警编号 */
    private String warningNo;

    /** 预警等级 */
    private String warningLevel;

    /** 预警标题 */
    private String title;

    /** 预警状态 */
    private String status;

    /** 管网类型 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联管网名称 */
    private String pipeName;

    /** 关联设备ID */
    private Long deviceId;

    /** 设备编码 */
    private String deviceCode;

    /** 定位经度 */
    private Double longitude;

    /** 定位纬度 */
    private Double latitude;

    /** 定位来源：DEVICE / PIPE / NONE */
    private String locationSource;

    /** 设备实时运行状态 */
    private Integer realtimeStatus;

    /** 实时采集时间戳（毫秒） */
    private Long realtimeTs;
}
