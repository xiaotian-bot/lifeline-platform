package com.tianxin.lifeline.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 原始传感数据消息 DTO。
 *
 * <p>字段契约与 lifeline-mock 的 {@code SensorData} 完全对齐，
 * 用于 {@code pipe_sensor_raw} Topic 消息反序列化（兼容单条 JSON 对象
 * 与压测模式的 JSON Array 两种消息体）。
 */
@Data
public class SensorRawMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（字符串设备ID），如 DEV-GAS-001 */
    private String deviceId;

    /** 管网类型编码：GAS / WATER / SEWAGE / HEAT / HAZARD（大小写均可，消费端归一化） */
    private String pipeType;

    /** 传感器类型：pressure / temperature / flow / vibration / leakage / corrosion */
    private String sensorType;

    /** 区域编码，如 AREA-001 */
    private String areaId;

    /** 数据采集时间戳（毫秒） */
    private Long ts;

    /** 压力（MPa） */
    private Double pressure;

    /** 温度（℃） */
    private Double temperature;

    /** 流量（m³/h） */
    private Double flow;

    /** 振动幅值（mm/s） */
    private Double vibration;

    /** 腐蚀程度（0~1，管壁减薄率） */
    private Double corrosion;

    /** 泄漏浓度/比率（燃气为 %LEL 近似值，正常接近 0） */
    private Double leakage;

    /** 设备状态：0 正常 / 1 异常 / 2 报警 */
    private Integer status;
}
