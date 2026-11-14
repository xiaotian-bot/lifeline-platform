package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单条预警定位视图（预警 + 设备/管线位置 + 可选实时值）。
 */
@Data
public class WarningLocationItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警事件ID */
    private Long warningId;

    /** 预警编号 */
    private String warningNo;

    /** 预警等级：BLUE/YELLOW/ORANGE/RED */
    private String warningLevel;

    /** 预警标题 */
    private String title;

    /** 预警状态 */
    private String status;

    /** 管网类型 */
    private String pipeType;

    /** 关联管网ID */
    private Long pipeId;

    /** 关联管网名称（资产侧降级时为 null） */
    private String pipeName;

    /** 关联设备ID（数据库主键） */
    private Long deviceId;

    /** 设备编码（用于实时值映射） */
    private String deviceCode;

    /** 定位经度（设备坐标优先，缺省取管线首点） */
    private Double longitude;

    /** 定位纬度 */
    private Double latitude;

    /** 定位来源：DEVICE 设备坐标 / PIPE 管线首点 / NONE 无坐标 */
    private String locationSource;

    /** 设备实时运行状态（0 正常 / 1 异常 / 2 报警，实时缓存缺失为 null） */
    private Integer realtimeStatus;

    /** 实时采集时间戳（毫秒，可为 null） */
    private Long realtimeTs;
}
