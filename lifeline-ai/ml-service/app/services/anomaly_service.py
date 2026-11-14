# -*- coding: utf-8 -*-
"""
异常检测业务服务。

编排 THREE_SIGMA / ISOLATION_FOREST / FUSION 三种算法，
TIMES_NET 在当前环境（无 GPU、未训练）下降级为 FUSION。
"""
import time
from typing import List, Optional, Tuple

import numpy as np

from app.algorithms import isolation_forest, three_sigma
from app.models.schemas import (
    AnomalyRequest,
    AnomalyResult,
    AnomalyType,
    ModelType,
)

# 融合权重：孤立森林 0.6，3σ 0.4
WEIGHT_IF = 0.6
WEIGHT_3SIGMA = 0.4
# 融合得分判定阈值
FUSION_THRESHOLD = 0.6


def detect(req: AnomalyRequest) -> AnomalyResult:
    """
    执行异常检测主流程。

    :param req: 异常检测请求
    :return: 结构化检测结果（camelCase 序列化由路由层处理）
    """
    start = time.perf_counter()
    series = req.series
    metric = req.metric_type.value

    degraded = False
    note: Optional[str] = None
    actual_model = req.model

    # ------------------------------------------------------------------
    # TIMES_NET 降级：当前环境无 GPU 且模型未训练，回退融合模型
    # ------------------------------------------------------------------
    if req.model == ModelType.TIMES_NET:
        degraded = True
        note = "TIMES_NET 模型未部署（无 GPU / 未训练），已降级为 FUSION 融合模型"
        actual_model = ModelType.FUSION

    # ------------------------------------------------------------------
    # 算法执行
    # ------------------------------------------------------------------
    if actual_model == ModelType.THREE_SIGMA:
        scores, indexes = three_sigma.detect(series)
    elif actual_model == ModelType.ISOLATION_FOREST:
        if_scores, indexes, skipped = isolation_forest.detect(series)
        if skipped:
            # 样本不足：降级为 3σ
            degraded = True
            note = f"样本数 {len(series)} < {isolation_forest.MIN_SAMPLES}，孤立森林降级为 3σ 统计"
            scores, indexes = three_sigma.detect(series)
        else:
            scores = if_scores  # type: ignore[assignment]
    else:  # FUSION（默认）
        scores, indexes, note = _fusion_detect(series)
        if note:
            degraded = True

    # ------------------------------------------------------------------
    # 汇总：取最大点得分作为序列级异常得分
    # ------------------------------------------------------------------
    anomaly_score = float(max(scores)) if scores else 0.0
    anomaly = bool(scores and max(scores) >= FUSION_THRESHOLD)
    if anomaly and not indexes:
        # 得分越限但单模型未标记时，补全下标
        indexes = [i for i, s in enumerate(scores) if s >= FUSION_THRESHOLD]

    anomaly_type = _infer_anomaly_type(series, indexes, metric) if anomaly else None

    # 置信度：得分越极端越可信；样本越多越可信
    sample_factor = min(len(series) / 30.0, 1.0)
    confidence = round(min(0.5 + anomaly_score * 0.4 + sample_factor * 0.1, 1.0), 4)

    cost_ms = int((time.perf_counter() - start) * 1000)

    return AnomalyResult(
        device_id=req.device_id,
        metric_type=metric,
        anomaly=anomaly,
        anomaly_score=round(anomaly_score, 4),
        anomaly_type=anomaly_type,
        confidence=confidence,
        anomaly_indexes=indexes,
        model=actual_model.value,
        cost_ms=cost_ms,
        degraded=degraded,
        note=note,
    )


def _fusion_detect(series: List[float]) -> Tuple[List[float], List[int], Optional[str]]:
    """
    融合检测：0.6 * 孤立森林 + 0.4 * 3σ。

    样本数不足时孤立森林跳过，退化为纯 3σ 并返回降级说明。
    """
    sig_scores, sig_indexes = three_sigma.detect(series)
    if_scores, if_indexes, skipped = isolation_forest.detect(series)

    if skipped:
        note = (
            f"样本数 {len(series)} < {isolation_forest.MIN_SAMPLES}，"
            f"融合模型退化为纯 3σ 统计"
        )
        return sig_scores, sig_indexes, note

    # 逐点加权融合
    fused = [
        WEIGHT_IF * a + WEIGHT_3SIGMA * b
        for a, b in zip(if_scores, sig_scores)  # type: ignore[arg-type]
    ]
    indexes = sorted(set(if_indexes) | set(sig_indexes))
    # 融合得分越限的点也纳入异常下标
    indexes = sorted(set(indexes) | {i for i, s in enumerate(fused) if s >= FUSION_THRESHOLD})
    return fused, indexes, None


def _infer_anomaly_type(
    series: List[float],
    indexes: List[int],
    metric: str,
) -> Optional[AnomalyType]:
    """
    异常类型规则推断：
        - pressure 突降幅度大 -> BURST（爆管），突降幅度中等 -> LEAK（泄漏）
        - leakage 报警        -> LEAK
        - corrosion 持续上升  -> CORROSION
        - flow 异常偏低       -> BLOCKAGE（堵塞）
        - 其他                -> None
    """
    arr = np.asarray(series, dtype=float)
    mean = float(np.mean(arr))

    if metric in ("pressure", "leakage"):
        # 关注异常点相对历史均值的相对跌幅
        anom_mean = float(np.mean(arr[indexes])) if indexes else float(arr.min())
        base = mean if abs(mean) > 1e-9 else 1.0
        drop = (base - anom_mean) / abs(base)
        if metric == "leakage":
            return AnomalyType.LEAK
        if drop >= 0.5:
            return AnomalyType.BURST   # 压力骤降超 50%：疑似爆管
        return AnomalyType.LEAK        # 压力下降：疑似泄漏

    if metric == "corrosion":
        # 腐蚀指标整体呈上升趋势 -> 腐蚀加剧
        if len(arr) >= 3 and float(arr[-1]) > float(arr[0]):
            return AnomalyType.CORROSION
        return AnomalyType.CORROSION

    if metric == "flow":
        # 流量显著低于均值 -> 堵塞
        anom_mean = float(np.mean(arr[indexes])) if indexes else float(arr.min())
        if abs(mean) > 1e-9 and anom_mean < mean:
            return AnomalyType.BLOCKAGE
        return None

    # temperature / vibration 暂无明确类型映射
    return None
