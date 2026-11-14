package com.tianxin.lifeline.warning.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预报报告分页查询请求。
 *
 * <p>支持按设备ID / 管网ID / 预报类型过滤。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ForecastQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联设备ID */
    private Long deviceId;

    /** 关联管网ID */
    private Long pipeId;

    /** 预报类型（如 LEAK_RISK / BURST_RISK / CORROSION_TREND） */
    private String forecastType;
}
