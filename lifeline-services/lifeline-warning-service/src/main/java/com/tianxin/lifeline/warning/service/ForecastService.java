package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.warning.dto.ForecastQueryRequest;
import com.tianxin.lifeline.warning.entity.ForecastReport;

/**
 * AI 预报报告查询服务。
 *
 * <p>预报数据由 AI 能力网关（P8 阶段）写入，本服务提供分页查询。
 */
public interface ForecastService {

    /**
     * 分页查询预报报告。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<ForecastReport> page(ForecastQueryRequest request);
}
