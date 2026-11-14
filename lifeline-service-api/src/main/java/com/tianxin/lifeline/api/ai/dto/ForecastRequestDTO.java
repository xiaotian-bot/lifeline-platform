package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 趋势预报请求 DTO（服务间共享）。
 *
 * <p>由 ai-gateway-service 转发至 ml-service 的
 * POST /api/v1/ai/forecast 能力。
 */
@Data
public class ForecastRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（如 DEV-GAS-001） */
    private String deviceCode;

    /** 管网类型：gas/water/drainage/heating/storm */
    private String pipeType;

    /** 指标类型：pressure/temperature/flow/vibration/corrosion/leakage */
    private String metricType;

    /** 历史时序数值序列（按时间升序） */
    private List<Double> series;

    /** 预报时长（天） */
    private Integer horizonDays;
}
