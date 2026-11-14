package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 时序异常检测请求 DTO（服务间共享）。
 */
@Data
public class AnomalyRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 设备编码（如 DEV-GAS-001，P8 新增，向后兼容可空） */
    private String deviceCode;

    /** 管网类型 */
    private String pipeType;

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String metricType;

    /** 时序数值序列（按时间升序） */
    private List<Double> series;

    /** 序列对应的起始时间戳（毫秒） */
    private Long startTime;

    /** 采样间隔（毫秒） */
    private Long interval;

    /** 检测模型：TIMES_NET / ISOLATION_FOREST / THREE_SIGMA / FUSION */
    private String model;
}
