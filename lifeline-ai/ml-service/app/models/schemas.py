# -*- coding: utf-8 -*-
"""
pydantic 数据模型定义。

请求/响应字段与 Java 侧（lifeline-ai-gateway-service）的
AnomalyRequestDTO / AnomalyResultDTO / ForecastRequestDTO / ForecastResultDTO 对齐。

兼容策略：
    - 响应统一使用 camelCase（alias），与 Java DTO 序列化结果一致；
    - 请求同时接受 camelCase 与 snake_case（populate_by_name=True）。
"""
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, ConfigDict, Field


class MetricType(str, Enum):
    """监测指标类型。"""

    PRESSURE = "pressure"          # 压力
    TEMPERATURE = "temperature"    # 温度
    FLOW = "flow"                  # 流量
    VIBRATION = "vibration"        # 振动
    CORROSION = "corrosion"        # 腐蚀
    LEAKAGE = "leakage"            # 泄漏


class ModelType(str, Enum):
    """异常检测算法模型。"""

    TIMES_NET = "TIMES_NET"              # TimesNet（深度学习，当前环境无 GPU，降级处理）
    ISOLATION_FOREST = "ISOLATION_FOREST"  # 孤立森林
    THREE_SIGMA = "THREE_SIGMA"          # 3σ 统计
    FUSION = "FUSION"                    # 融合模型（默认）


class AnomalyType(str, Enum):
    """异常类型（规则推断结果）。"""

    LEAK = "LEAK"            # 泄漏
    BURST = "BURST"          # 爆管
    CORROSION = "CORROSION"  # 腐蚀
    BLOCKAGE = "BLOCKAGE"    # 堵塞


class ForecastType(str, Enum):
    """预报类型。"""

    RUL = "RUL"                        # 剩余寿命预测
    DEGRADATION = "DEGRADATION"        # 性能退化趋势
    TREND = "TREND"                    # 一般趋势预报


# ---------------------------------------------------------------------------
# 异常检测
# ---------------------------------------------------------------------------

class AnomalyRequest(BaseModel):
    """异常检测请求体（对齐 AnomalyRequestDTO）。"""

    model_config = ConfigDict(populate_by_name=True)

    device_id: Optional[int] = Field(default=None, alias="deviceId", description="设备ID，可空")
    device_code: str = Field(default="", alias="deviceCode", description="设备编码")
    pipe_type: str = Field(default="", alias="pipeType", description="管网类型 GAS/WATER/HEATING/DRAINAGE")
    metric_type: MetricType = Field(alias="metricType", description="指标类型")
    series: List[float] = Field(..., min_length=1, description="时序数据")
    start_time: Optional[str] = Field(default=None, alias="startTime", description="起始时间")
    interval: Optional[int] = Field(default=None, description="采样间隔（秒）")
    model: ModelType = Field(default=ModelType.FUSION, description="检测模型，默认 FUSION")


class AnomalyResult(BaseModel):
    """异常检测结果（对齐 AnomalyResultDTO）。"""

    model_config = ConfigDict(populate_by_name=True)

    device_id: Optional[int] = Field(default=None, alias="deviceId")
    metric_type: str = Field(alias="metricType")
    anomaly: bool = Field(description="是否异常")
    anomaly_score: float = Field(alias="anomalyScore", ge=0, le=1, description="异常得分 0-1")
    anomaly_type: Optional[AnomalyType] = Field(default=None, alias="anomalyType")
    confidence: float = Field(ge=0, le=1, description="置信度 0-1")
    anomaly_indexes: List[int] = Field(default_factory=list, alias="anomalyIndexes")
    model: str = Field(description="实际生效模型（TIMES_NET 降级时为 FUSION）")
    cost_ms: int = Field(default=0, alias="costMs", description="算法耗时（毫秒）")
    degraded: bool = Field(default=False, description="是否为降级结果")
    note: Optional[str] = Field(default=None, description="补充说明（如降级原因）")

    def to_camel_dict(self) -> dict:
        """按 camelCase 别名序列化，与 Java 侧字段名保持一致。"""
        return self.model_dump(by_alias=True)


# ---------------------------------------------------------------------------
# 健康预报
# ---------------------------------------------------------------------------

class ForecastRequest(BaseModel):
    """健康预报请求体（对齐 ForecastRequestDTO）。"""

    model_config = ConfigDict(populate_by_name=True)

    device_code: str = Field(default="", alias="deviceCode")
    pipe_type: str = Field(default="", alias="pipeType")
    metric_type: MetricType = Field(alias="metricType")
    series: List[float] = Field(..., min_length=1, description="历史时序数据")
    horizon_days: int = Field(default=90, alias="horizonDays", ge=1, le=3650, description="预报天数，默认 90")


class ForecastResult(BaseModel):
    """健康预报结果（对齐 ForecastResultDTO）。"""

    model_config = ConfigDict(populate_by_name=True)

    forecast_type: ForecastType = Field(alias="forecastType")
    trend_desc: str = Field(alias="trendDesc", description="中文趋势描述")
    risk_time: Optional[str] = Field(default=None, alias="riskTime", description="预计风险时间（ISO），可空")
    confidence: float = Field(ge=0, le=1)
    report_content: str = Field(alias="reportContent", description="中文预报报告")
    forecast_series: List[float] = Field(default_factory=list, alias="forecastSeries")
    cost_ms: int = Field(default=0, alias="costMs")

    def to_camel_dict(self) -> dict:
        """按 camelCase 别名序列化。"""
        return self.model_dump(by_alias=True)


# ---------------------------------------------------------------------------
# 统一响应包装（与 Java 侧 Result<T> 对齐：code / message / data）
# ---------------------------------------------------------------------------

class ApiResult(BaseModel):
    """统一响应结构。"""

    code: int = 200
    message: str = "success"
    data: Optional[dict] = None
