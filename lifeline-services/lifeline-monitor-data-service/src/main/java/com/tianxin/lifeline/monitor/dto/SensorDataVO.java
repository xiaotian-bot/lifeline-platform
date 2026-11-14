package com.tianxin.lifeline.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感监测数据响应 DTO。
 *
 * <p>字段结构与 service-api 模块的 {@code SensorDataDTO} 完全一致
 * （Feign 契约走 JSON over HTTP，字段名/类型一致即互相兼容）。
 * 本模块 pom 未依赖 lifeline-service-api 且约定不修改 pom，
 * 故在此定义同构本地 DTO 供 Controller 与查询服务使用。
 */
@Data
public class SensorDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（字符串设备ID，如 DEV-GAS-001，对应 TDengine 标签 device_id） */
    private String deviceId;

    /** 管网类型（TDengine 标签 pipe_type） */
    private String pipeType;

    /** 传感器类型（TDengine 标签 sensor_type） */
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

    /** 腐蚀度（0~1，管壁减薄率） */
    private Double corrosion;

    /** 泄漏指标（燃气为 %LEL 近似值） */
    private Double leakage;

    /** 设备运行状态：0 正常 / 1 异常 / 2 报警（与上报契约一致） */
    private Integer status;
}
