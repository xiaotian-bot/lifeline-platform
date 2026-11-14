package com.tianxin.lifeline.warning.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 原始传感数据消息 DTO。
 *
 * <p>字段与 lifeline-mock 的 SensorData / pipe_sensor_raw 消息契约对齐，
 * 供规则判定桥解析使用。忽略未知字段以兼容契约演进。
 */
@Data
public class SensorRawMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID，如 DEV-GAS-001 */
    private String deviceId;

    /** 管网类型编码：GAS / WATER / SEWAGE / HEAT / HAZARD */
    private String pipeType;

    /** 传感器类型：pressure / temperature / flow / vibration / leakage / corrosion */
    private String sensorType;

    /** 区域ID，如 AREA-001 */
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

    /** 泄漏浓度/比率 */
    private Double leakage;

    /** 设备状态：0 正常 / 1 异常 / 2 报警 */
    private Integer status;

    /**
     * 按指标类型提取对应的监测值。
     *
     * @param metricType 指标类型
     * @return 监测值，无对应字段返回 null
     */
    public Double metricValue(String metricType) {
        if (metricType == null) {
            return null;
        }
        return switch (metricType.toLowerCase()) {
            case "pressure" -> pressure;
            case "temperature" -> temperature;
            case "flow" -> flow;
            case "vibration" -> vibration;
            case "corrosion" -> corrosion;
            case "leakage" -> leakage;
            default -> null;
        };
    }
}
