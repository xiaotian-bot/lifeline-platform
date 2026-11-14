package com.tianxin.lifeline.warning.controller;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.warning.dto.ForecastQueryRequest;
import com.tianxin.lifeline.warning.entity.ForecastReport;
import com.tianxin.lifeline.warning.service.ForecastService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 预报报告 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/forecasts，Controller 只做参数校验与 Result 包装。
 */
@RestController
@RequestMapping("/api/v1/forecasts")
public class ForecastController {

    /** 预报报告查询服务 */
    private final ForecastService forecastService;

    /**
     * 构造控制器。
     *
     * @param forecastService 预报报告查询服务
     */
    public ForecastController(ForecastService forecastService) {
        this.forecastService = forecastService;
    }

    /**
     * 分页查询预报报告。
     *
     * @param request 分页与过滤条件（设备ID/管网ID/预报类型）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<ForecastReport>> page(@Valid ForecastQueryRequest request) {
        return Result.success(forecastService.page(request));
    }
}
