package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 趋势预报结果 DTO（服务间共享）。
 *
 * <p>ml-service POST /api/v1/ai/forecast 的出参契约。
 * 非降级结果由 ai-gateway-service 直写 forecast_report 表。
 */
@Data
public class ForecastResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预报类型：如 TREND/RISK/STABLE */
    private String forecastType;

    /** 趋势描述 */
    private String trendDesc;

    /** 风险时间（预测达到风险阈值的时间，字符串形式） */
    private String riskTime;

    /** 置信度（0-1） */
    private Double confidence;

    /** 预报报告正文（Markdown/纯文本） */
    private String reportContent;

    /** 预报序列（未来时间点预测值） */
    private List<ForecastPointDTO> forecastSeries;
}
