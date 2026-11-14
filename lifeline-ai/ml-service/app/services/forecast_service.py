# -*- coding: utf-8 -*-
"""
健康预报业务服务。

算法组合：线性回归外推 + 灰色模型 GM(1,1) 加权融合，
按各指标经验阈值估算风险时间（riskTime），并生成中文预报报告。
"""
import time
from datetime import datetime, timedelta
from typing import List, Optional, Tuple

import numpy as np

from app.algorithms import gm11
from app.models.schemas import ForecastRequest, ForecastResult, ForecastType

# 线性回归与灰色模型的融合权重
WEIGHT_LR = 0.6
WEIGHT_GM = 0.4

# ---------------------------------------------------------------------------
# 各指标类型的经验阈值与方向：
#   direction = "low"  : 指标低于阈值视为风险（如压力过低）
#   direction = "high" : 指标高于阈值视为风险（如腐蚀速率过高）
# 阈值为工程经验值，单位与上报数据一致。
# ---------------------------------------------------------------------------
RISK_RULES = {
    "pressure":    {"direction": "low",  "threshold": 0.15, "unit": "MPa",   "hidden": "低压/泄漏隐患"},
    "temperature": {"direction": "high", "threshold": 95.0, "unit": "℃",    "hidden": "超温运行隐患"},
    "flow":        {"direction": "low",  "threshold": 10.0, "unit": "m³/h", "hidden": "流量衰减/堵塞隐患"},
    "vibration":   {"direction": "high", "threshold": 7.1,  "unit": "mm/s",  "hidden": "振动超标/结构松动隐患"},
    "corrosion":   {"direction": "high", "threshold": 0.8,  "unit": "腐蚀指数", "hidden": "腐蚀劣化隐患"},
    "leakage":     {"direction": "high", "threshold": 5.0,  "unit": "泄漏指数", "hidden": "泄漏风险"},
}

# metricType -> 预报类型映射
FORECAST_TYPE_MAP = {
    "corrosion": ForecastType.RUL,           # 腐蚀 -> 剩余寿命
    "vibration": ForecastType.DEGRADATION,   # 振动 -> 性能退化
    "pressure": ForecastType.DEGRADATION,
    "leakage": ForecastType.DEGRADATION,
    "temperature": ForecastType.TREND,
    "flow": ForecastType.TREND,
}

METRIC_CN = {
    "pressure": "压力", "temperature": "温度", "flow": "流量",
    "vibration": "振动", "corrosion": "腐蚀", "leakage": "泄漏",
}


def forecast(req: ForecastRequest) -> ForecastResult:
    """
    健康预报主流程。

    :param req: 预报请求（历史序列 + 预报天数）
    :return: 结构化预报结果
    """
    start = time.perf_counter()
    series = req.series
    horizon = req.horizon_days
    metric = req.metric_type.value

    # ------------------------------------------------------------------
    # 1. 线性回归外推
    # ------------------------------------------------------------------
    lr_series, slope, lr_r2 = _linear_extrapolate(series, horizon)

    # ------------------------------------------------------------------
    # 2. GM(1,1) 灰色模型（数据 >= 4 点且为正序列时启用，否则平移处理）
    # ------------------------------------------------------------------
    gm_series, gm_mae = _gm_forecast(series, horizon)

    # ------------------------------------------------------------------
    # 3. 加权融合
    # ------------------------------------------------------------------
    if gm_series is not None:
        forecast_series = [
            round(WEIGHT_LR * a + WEIGHT_GM * b, 6)
            for a, b in zip(lr_series, gm_series)
        ]
        confidence = _calc_confidence(lr_r2, gm_mae, len(series), dual=True)
    else:
        forecast_series = [round(v, 6) for v in lr_series]
        confidence = _calc_confidence(lr_r2, None, len(series), dual=False)

    # ------------------------------------------------------------------
    # 4. 风险时间估算与报告生成
    # ------------------------------------------------------------------
    rule = RISK_RULES.get(metric)
    risk_day, risk_time = _estimate_risk_time(series, forecast_series, rule)
    trend_desc = _describe_trend(metric, slope, series, forecast_series)
    report = _build_report(req, metric, trend_desc, risk_time, risk_day, rule, confidence)

    cost_ms = int((time.perf_counter() - start) * 1000)

    return ForecastResult(
        forecast_type=FORECAST_TYPE_MAP.get(metric, ForecastType.TREND),
        trend_desc=trend_desc,
        risk_time=risk_time,
        confidence=confidence,
        report_content=report,
        forecast_series=forecast_series,
        cost_ms=cost_ms,
    )


# ---------------------------------------------------------------------------
# 内部实现
# ---------------------------------------------------------------------------

def _linear_extrapolate(series: List[float], horizon: int) -> Tuple[List[float], float, float]:
    """
    一元线性回归外推。

    :return: (预测序列, 斜率, 拟合优度 R²)
    """
    arr = np.asarray(series, dtype=float)
    x = np.arange(len(arr), dtype=float)
    # 一次多项式拟合
    k, b = np.polyfit(x, arr, 1)
    # R² 拟合优度
    pred = k * x + b
    ss_res = float(np.sum((arr - pred) ** 2))
    ss_tot = float(np.sum((arr - arr.mean()) ** 2))
    r2 = 1.0 - ss_res / ss_tot if ss_tot > 1e-12 else 1.0
    # 向前外推
    future_x = np.arange(len(arr), len(arr) + horizon, dtype=float)
    future = (k * future_x + b).tolist()
    return future, float(k), max(r2, 0.0)


def _gm_forecast(series: List[float], horizon: int) -> Tuple[Optional[List[float]], Optional[float]]:
    """
    GM(1,1) 预报。数据不满足正序列条件时做整体平移，预测后再还原；
    样本不足 4 点则跳过（返回 None）。

    :return: (预测序列 或 None, 平均相对误差 或 None)
    """
    if len(series) < gm11.MIN_SAMPLES:
        return None, None

    arr = np.asarray(series, dtype=float)
    shift = 0.0
    if arr.min() <= 0:
        # 平移使序列为正，保证累加生成有效
        shift = abs(arr.min()) + 1.0

    shifted = [float(v) + shift for v in arr]
    try:
        preds, mae = gm11.predict(shifted, horizon)
    except Exception:
        return None, None

    preds = [p - shift for p in preds]
    # GM(1,1) 误差过大时视为不可信
    if mae > 0.5:
        return None, None
    return preds, mae


def _calc_confidence(r2: float, gm_mae: Optional[float], n: int, dual: bool) -> float:
    """根据拟合优度、灰色模型误差与样本量估算置信度（0-1）。"""
    score = 0.4 + 0.35 * r2 + min(n / 60.0, 1.0) * 0.15
    if dual and gm_mae is not None:
        score += 0.1 * max(0.0, 1.0 - gm_mae * 2)
    return round(min(max(score, 0.3), 0.98), 4)


def _estimate_risk_time(
    history: List[float],
    forecast_series: List[float],
    rule: Optional[dict],
) -> Tuple[Optional[int], Optional[str]]:
    """
    按经验阈值估算风险时间。

    :return: (风险日偏移 或 None, ISO 格式时间字符串 或 None)
    """
    if not rule or not forecast_series:
        return None, None

    threshold = rule["threshold"]
    direction = rule["direction"]

    # 历史数据已越限：风险视为已经出现
    last = history[-1]
    if (direction == "low" and last < threshold) or (direction == "high" and last > threshold):
        return 0, datetime.now().isoformat(timespec="seconds")

    for day, v in enumerate(forecast_series, start=1):
        if (direction == "low" and v < threshold) or (direction == "high" and v > threshold):
            risk_dt = datetime.now() + timedelta(days=day)
            return day, risk_dt.isoformat(timespec="seconds")
    return None, None


def _describe_trend(
    metric: str,
    slope: float,
    series: List[float],
    forecast_series: List[float],
) -> str:
    """生成中文趋势描述。"""
    mean = float(np.mean(series))
    base = abs(mean) if abs(mean) > 1e-9 else 1.0
    daily_pct = slope / base * 100.0  # 日均变化率（%）

    if abs(daily_pct) < 0.05:
        state = "整体平稳"
    elif daily_pct > 0:
        state = "持续上升" if daily_pct >= 0.5 else "缓慢上升"
    else:
        state = "持续下降" if daily_pct <= -0.5 else "缓慢下降"

    last_val = forecast_series[-1] if forecast_series else series[-1]
    return (
        f"{METRIC_CN.get(metric, metric)}指标{state}"
        f"（日均变化率约 {daily_pct:+.2f}%），"
        f"预计 {len(forecast_series)} 天后数值约为 {last_val:.4f}"
    )


def _build_report(
    req: ForecastRequest,
    metric: str,
    trend_desc: str,
    risk_time: Optional[str],
    risk_day: Optional[int],
    rule: Optional[dict],
    confidence: float,
) -> str:
    """生成中文预报报告（隐患类型 / 趋势 / 建议）。"""
    metric_cn = METRIC_CN.get(metric, metric)
    hidden = rule["hidden"] if rule else "运行状态波动隐患"
    unit = rule["unit"] if rule else ""

    lines = [
        f"【健康预报报告】设备编码：{req.device_code or '未知'}，管网类型：{req.pipe_type or '未知'}",
        f"一、隐患类型：{hidden}。",
        f"二、趋势分析：{trend_desc}。",
    ]

    if risk_time:
        if risk_day == 0:
            lines.append(f"三、风险提示：当前{metric_cn}指标已越过经验阈值"
                         f"（{rule['threshold']} {unit}），风险已经显现，建议立即处置。")
        else:
            lines.append(f"三、风险提示：按当前趋势外推，预计约 {risk_day} 天后"
                         f"（{risk_time}）{metric_cn}指标将达到风险阈值"
                         f"（{rule['threshold']} {unit}）。")
    else:
        lines.append(f"三、风险提示：预报期内（{req.horizon_days} 天）{metric_cn}指标"
                     f"未触及经验风险阈值，暂无明确风险时间点。")

    suggestion = {
        "pressure": "建议加密压力监测频次，排查沿线阀门与接口密封性，必要时安排听漏检测。",
        "temperature": "建议检查供热/伴热系统运行工况，核查温度传感器漂移情况。",
        "flow": "建议核查流量计与管段通径，排查淤积、结垢或局部堵塞情况。",
        "vibration": "建议复核管道支吊架与固定墩状态，评估减振加固措施。",
        "corrosion": "建议开展腐蚀速率复测与壁厚检测，评估阴极保护有效性，制定修复计划。",
        "leakage": "建议立即安排现场巡检与泄漏点定位，准备应急抢修物资。",
    }.get(metric, "建议持续跟踪监测，发现异常及时上报处置。")

    lines.append(f"四、处置建议：{suggestion}")
    lines.append(f"五、模型置信度：{confidence:.2%}（线性回归外推 + GM(1,1) 灰色模型加权融合）。")
    return "\n".join(lines)
