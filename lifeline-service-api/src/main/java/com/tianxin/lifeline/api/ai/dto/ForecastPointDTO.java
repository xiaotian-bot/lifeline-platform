package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 预报序列点 DTO（服务间共享）。
 *
 * <p>forecastSeries 中的单个预报点：预测时间与预测值。
 */
@Data
public class ForecastPointDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预测时间点（ISO-8601 字符串，如 2026-08-20T12:00:00） */
    private String time;

    /** 预测值 */
    private Double value;
}
