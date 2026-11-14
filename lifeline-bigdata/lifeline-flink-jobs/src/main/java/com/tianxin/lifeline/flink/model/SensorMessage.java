package com.tianxin.lifeline.flink.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感数据统一模型（Flink 链路内部流转）。
 *
 * <p>字段与 lifeline-mock 的 SensorData / pipe_sensor_raw 消息契约、
 * warning-service 的 SensorRawMessage 完全对齐；SensorCleanJob 清洗后
 * 以相同字段结构序列化输出至 pipe_sensor_clean（下游 monitor/warning
 * 服务现有 DTO 可直接反序列化，无需任何改动）。
 */
@Data
public class SensorMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 6 类监测指标名（与 sensor_data 超级表列序一致） */
    public static final String[] METRICS =
            {"pressure", "temperature", "flow", "vibration", "corrosion", "leakage"};

    /** 设备ID，如 DEV-GAS-001 */
    private String deviceId;

    /** 管网类型编码：GAS / WATER / SEWAGE / HEAT / HAZARD（清洗后归一化为大写枚举码） */
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
     * 按指标类型提取对应的监测值（与 warning-service SensorRawMessage.metricValue 语义一致）。
     *
     * @param metricType 指标类型（大小写不敏感）
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
