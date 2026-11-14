# -*- coding: utf-8 -*-
"""
AI 算法接口路由。

接口清单：
    POST /api/v1/ai/anomaly-detect  时序异常检测
    POST /api/v1/ai/forecast        健康预报（趋势外推 + 灰色模型）

统一返回结构（与 Java 侧 Result<T> 对齐）：{"code": 200, "message": "...", "data": {...}}
"""
from fastapi import APIRouter, HTTPException

from app.models.schemas import (
    AnomalyRequest,
    ApiResult,
    ForecastRequest,
)
from app.services import anomaly_service, forecast_service

router = APIRouter(prefix="/api/v1/ai", tags=["AI 算法服务"])

# 时序数据最小长度（不足直接 400）
MIN_SERIES_LEN = 3


@router.post("/anomaly-detect", response_model=ApiResult, summary="时序异常检测")
def anomaly_detect(req: AnomalyRequest) -> ApiResult:
    """
    对上报的监测时序数据执行异常检测。

    支持 THREE_SIGMA / ISOLATION_FOREST / FUSION（默认）模型；
    TIMES_NET 在无 GPU 环境下自动降级为 FUSION。
    """
    if len(req.series) < MIN_SERIES_LEN:
        raise HTTPException(
            status_code=400,
            detail=f"series 长度不足：至少需要 {MIN_SERIES_LEN} 个数据点，实际 {len(req.series)}",
        )
    result = anomaly_service.detect(req)
    return ApiResult(code=200, message="success", data=result.to_camel_dict())


@router.post("/forecast", response_model=ApiResult, summary="健康预报")
def forecast(req: ForecastRequest) -> ApiResult:
    """
    基于历史时序数据做趋势外推与健康预报。

    线性回归 + GM(1,1) 灰色模型加权融合，输出趋势描述、
    风险时间估算与中文报告。
    """
    if len(req.series) < MIN_SERIES_LEN:
        raise HTTPException(
            status_code=400,
            detail=f"series 长度不足：至少需要 {MIN_SERIES_LEN} 个数据点，实际 {len(req.series)}",
        )
    result = forecast_service.forecast(req)
    return ApiResult(code=200, message="success", data=result.to_camel_dict())
