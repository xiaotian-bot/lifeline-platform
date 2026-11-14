# -*- coding: utf-8 -*-
"""
孤立森林（Isolation Forest）异常检测算法。

说明：
    - 使用 scikit-learn 的 IsolationForest，contamination=0.05；
    - 样本数 < 10 时模型不可靠，返回 skipped=True，由上层做融合降级；
    - sklearn 的 score_samples 返回论文定义异常分数的负值
      （越异常越负，正常样本约在 -0.5 附近），
      这里取负并做 min-max 归一化映射到 [0, 1]。
"""
from typing import List, Optional, Tuple

import numpy as np
from sklearn.ensemble import IsolationForest

# 最小样本数：低于该值孤立森林不具备统计意义
MIN_SAMPLES = 10


def detect(series: List[float]) -> Tuple[Optional[List[float]], List[int], bool]:
    """
    孤立森林异常检测。

    :param series: 时序数据
    :return: (逐点异常得分列表[0-1] 或 None, 异常点下标列表, 是否跳过)
    """
    n = len(series)
    if n < MIN_SAMPLES:
        return None, [], True

    arr = np.asarray(series, dtype=float).reshape(-1, 1)

    model = IsolationForest(
        n_estimators=100,
        contamination=0.05,
        random_state=42,
    )
    labels = model.fit_predict(arr)            # 1=正常, -1=异常
    raw = -model.score_samples(arr)            # 转为论文语义：越大越异常

    # min-max 归一化到 [0, 1]；同时保证正常区间得分集中在低位
    lo, hi = float(raw.min()), float(raw.max())
    if hi - lo < 1e-9:
        scores = [0.0] * n
    else:
        scores = [float((v - lo) / (hi - lo)) for v in raw]

    indexes = [i for i, lb in enumerate(labels) if lb == -1]
    # 被模型判定异常的点得分下限抬升至 0.6，保持与判定阈值语义一致
    for i in indexes:
        scores[i] = max(scores[i], 0.6)

    return scores, indexes, False
