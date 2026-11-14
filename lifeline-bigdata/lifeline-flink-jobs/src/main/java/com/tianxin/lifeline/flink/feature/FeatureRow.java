package com.tianxin.lifeline.flink.feature;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 窗口特征结果行（写入 TDengine sensor_feature 超级表）。
 *
 * <p>一行 = 一个设备某指标在某窗口粒度下的统计特征：
 * avg / peak / std / rate_of_change，附带样本数与窗口起止时间。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRow implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 窗口结束时间戳（毫秒，作为 TDengine 记录主时间轴） */
    private Long ts;

    /** 指标名：pressure / temperature / flow / vibration / corrosion / leakage */
    private String metricName;

    /** 窗口内均值 */
    private Double avgValue;

    /** 窗口内峰值（最大值） */
    private Double peakValue;

    /** 窗口内标准差 */
    private Double stdValue;

    /** 变化率：(末值-首值)/窗口秒数，反映趋势斜率 */
    private Double rateOfChange;

    /** 窗口内样本数 */
    private Integer sampleCount;

    /** 窗口起始时间戳（毫秒） */
    private Long windowStart;

    /** 窗口结束时间戳（毫秒） */
    private Long windowEnd;

    /* ==================== TAGS ==================== */

    /** 设备编码 */
    private String deviceId;

    /** 管网类型编码 */
    private String pipeType;

    /** 传感器类型 */
    private String sensorType;

    /** 窗口粒度：1m / 5m / 1h */
    private String intervalType;
}
