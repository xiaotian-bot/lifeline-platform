package com.tianxin.lifeline.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 预报报告实体（对应表 forecast_report）。
 *
 * <p>由 AI 能力网关（P8 阶段）产出趋势预报写入，本服务提供分页查询能力。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("forecast_report")
public class ForecastReport extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联管网ID */
    private Long pipeId;

    /** 预报类型（如：LEAK_RISK / BURST_RISK / CORROSION_TREND） */
    private String forecastType;

    /** 趋势描述 */
    private String trendDesc;

    /** 预测风险发生时间 */
    private LocalDateTime riskTime;

    /** 置信度（0~1） */
    private BigDecimal confidence;

    /** 预报报告正文 */
    private String reportContent;
}
