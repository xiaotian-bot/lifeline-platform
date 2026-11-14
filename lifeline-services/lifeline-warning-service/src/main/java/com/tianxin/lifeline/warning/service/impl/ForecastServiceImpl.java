package com.tianxin.lifeline.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.warning.dto.ForecastQueryRequest;
import com.tianxin.lifeline.warning.entity.ForecastReport;
import com.tianxin.lifeline.warning.mapper.ForecastReportMapper;
import com.tianxin.lifeline.warning.service.ForecastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 预报报告查询服务实现。
 */
@Slf4j
@Service
public class ForecastServiceImpl implements ForecastService {

    /** 预报报告 Mapper */
    private final ForecastReportMapper forecastReportMapper;

    /**
     * 构造预报查询服务。
     *
     * @param forecastReportMapper 预报报告 Mapper
     */
    public ForecastServiceImpl(ForecastReportMapper forecastReportMapper) {
        this.forecastReportMapper = forecastReportMapper;
    }

    /**
     * 分页查询预报报告（按创建时间倒序）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<ForecastReport> page(ForecastQueryRequest request) {
        LambdaQueryWrapper<ForecastReport> wrapper = new LambdaQueryWrapper<>();
        if (request.getDeviceId() != null) {
            wrapper.eq(ForecastReport::getDeviceId, request.getDeviceId());
        }
        if (request.getPipeId() != null) {
            wrapper.eq(ForecastReport::getPipeId, request.getPipeId());
        }
        if (request.getForecastType() != null && !request.getForecastType().isBlank()) {
            wrapper.eq(ForecastReport::getForecastType, request.getForecastType().trim());
        }
        wrapper.orderByDesc(ForecastReport::getCreatedAt);

        Page<ForecastReport> page = new Page<>(request.getPage(), request.getSize());
        Page<ForecastReport> result = forecastReportMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), request.getPage(), request.getSize());
    }
}
