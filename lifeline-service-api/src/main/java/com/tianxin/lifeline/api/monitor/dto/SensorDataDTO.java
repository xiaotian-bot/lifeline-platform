package com.tianxin.lifeline.api.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感监测数据 DTO（服务间共享）。
 *
 * <p>对应 TDengine 超级表 sensor_data 的字段结构。
 */
@Data
public class SensorDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（字符串设备ID，如 DEV-GAS-001，对应 TDengine 标签 device_id） */
    private String deviceId;

    /** 管网类型（TDengine 标签） */
    private String pipeType;

    /** 传感器类型（TDengine 标签） */
    private String sensorType;

    /** 所属区域编码（TDengine 标签 area_id，如 AREA-001） */
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

    /** 设备运行状态：0 正常 / 1 异常 / 2 报警（与上报契约一致） */
    private Integer status;
}
