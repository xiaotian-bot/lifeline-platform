package com.tianxin.lifeline.monitor.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 传感监测数据统计响应 DTO。
 *
 * <p>对应 TDengine INTERVAL 聚合查询的单个时间窗口结果，
 * 字段结构与 service-api 模块的 {@code SensorStatisticsDTO} 完全一致
 * （Feign 契约走 JSON over HTTP，字段名/类型一致即互相兼容）。
 */
@Data
public class SensorStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（字符串设备ID，如 DEV-GAS-001） */
    private String deviceId;

    /** 统计窗口起始时间（毫秒时间戳） */
    private Long ts;

    /** 窗口内样本条数 */
    private Long count;

    /** 压力最大值（MPa） */
    private Double maxPressure;

    /** 压力最小值（MPa） */
    private Double minPressure;

    /** 压力平均值（MPa） */
    private Double avgPressure;

    /** 温度最大值（℃） */
    private Double maxTemperature;

    /** 温度最小值（℃） */
    private Double minTemperature;

    /** 温度平均值（℃） */
    private Double avgTemperature;

    /** 流量最大值（m³/h） */
    private Double maxFlow;

    /** 流量最小值（m³/h） */
    private Double minFlow;

    /** 流量平均值（m³/h） */
    private Double avgFlow;

    /** 振动最大值（mm/s） */
    private Double maxVibration;

    /** 振动最小值（mm/s） */
    private Double minVibration;

    /** 振动平均值（mm/s） */
    private Double avgVibration;

    /** 腐蚀度最大值 */
    private Double maxCorrosion;

    /** 腐蚀度最小值 */
    private Double minCorrosion;

    /** 腐蚀度平均值 */
    private Double avgCorrosion;

    /** 泄漏指标最大值 */
    private Double maxLeakage;

    /** 泄漏指标最小值 */
    private Double minLeakage;

    /** 泄漏指标平均值 */
    private Double avgLeakage;
}
