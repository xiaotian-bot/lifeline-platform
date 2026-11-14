package com.tianxin.lifeline.api.asset.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 附属设备档案 DTO（服务间共享）。
 */
@Data
public class DeviceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备编码 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型：井盖/阀门/泵站/传感器等 */
    private String deviceType;

    /** 传感器类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String sensorType;

    /** 所属管网ID */
    private Long pipeId;

    /** 管网类型 */
    private String pipeType;

    /** 所属区域ID */
    private Long areaId;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 通信协议：LoRaWAN/MQTT/Modbus/NB-IoT 等 */
    private String protocol;

    /** 设备状态：0=离线 / 1=在线 / 2=故障 */
    private Integer status;

    /** 安装时间 */
    private LocalDateTime installDate;
}
