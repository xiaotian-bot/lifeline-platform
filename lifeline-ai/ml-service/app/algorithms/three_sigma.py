# -*- coding: utf-8 -*-
"""
3σ（三西格玛）统计异常检测算法。

原理：
    假设时序数据近似服从正态分布，正常数据落在 均值 ± 3σ 区间内的
    概率约为 99.73%，越限点判定为异常。
    单点得分 = 归一化偏离度：|x - mean| / (6σ)，截断到 [0, 1]。
"""
from typing import List, Tuple

import numpy as np


def detect(series: List[float]) -> Tuple[List[float], List[int]]:
    """
    3σ 异常检测。

    :param series: 时序数据（长度 >= 3）
    :return: (逐点异常得分列表[0-1], 异常点下标列表)
    """
    arr = np.asarray(series, dtype=float)
    mean = float(np.mean(arr))
    std = float(np.std(arr))

    # 标准差为 0（恒定序列）：所有点均正常
    if std < 1e-9:
        return [0.0] * len(series), []

    # 归一化偏离度：3σ 对应得分 0.5，6σ 对应得分 1.0
    scores = [min(abs(float(x) - mean) / (6.0 * std), 1.0) for x in arr]

    # 越限判定：超出 均值 ± 3σ
    upper, lower = mean + 3.0 * std, mean - 3.0 * std
    indexes = [i for i, x in enumerate(arr) if x > upper or x < lower]
    # 越限点得分至少 0.6，保证判定与得分语义一致
    for i in indexes:
        scores[i] = max(scores[i], 0.6)

    return scores, indexes
