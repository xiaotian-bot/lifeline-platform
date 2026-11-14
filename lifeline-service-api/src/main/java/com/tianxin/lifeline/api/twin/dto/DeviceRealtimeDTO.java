package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 单设备实时状态 DTO（服务间共享）。
 */
@Data
public class DeviceRealtimeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码 */
    private String deviceId;

    /** 管网类型 */
    private String pipeType;

    /** 传感器类型 */
    private String sensorType;

    /** 所属区域编码 */
    private String areaId;

    /** 数据采集时间戳（毫秒） */
    private Long ts;

    /** 压力值（MPa） */
    private Double pressure;

    /** 温度值（℃） */
    private Double temperature;

    /** 流量值（m³/h） */
    private Double flow;

    /** 振动值（mm/s） */
    private Double vibration;

    /** 腐蚀度（mm/年） */
    private Double corrosion;

    /** 泄漏指标 */
    private Double leakage;

    /** 设备运行状态：0 正常 / 1 异常 / 2 报警 */
    private Integer status;
}
