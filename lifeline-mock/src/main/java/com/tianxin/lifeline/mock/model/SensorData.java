package com.tianxin.lifeline.mock.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感器数据模型。
 *
 * <p>Mock 系统生成的单条原始传感数据，以 JSON 形式上报至 Kafka Topic
 * {@code pipe_sensor_raw}，字段与下游 Flink 清洗作业的输入契约对齐。
 *
 * <p>不同管网类型关注的指标不同，不涉及的指标字段保持 0 或 null：
 * 供水/燃气/供暖/危废关注 pressure，污水关注 flow，燃气额外关注 leakage。
 */
@Data
public class SensorData implements Serializable {

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
    private long ts;

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
