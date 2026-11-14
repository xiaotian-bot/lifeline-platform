# -*- coding: utf-8 -*-
"""
GM(1,1) 灰色预测模型（自实现，无第三方依赖）。

适用条件：
    - 样本数 >= 4；
    - 原始数据需为正数序列（非正数据由调用方做平移处理）。

原理概述：
    一次累加生成（1-AGO）弱化随机性 -> 紧邻均值生成 -> 最小二乘估计
    发展系数 a 与灰作用量 b -> 求解白化微分方程得到累加序列预测值
    -> 累减还原得到原始序列预测值。
"""
from typing import List, Tuple

import numpy as np

MIN_SAMPLES = 4


def is_applicable(series: List[float]) -> bool:
    """判断序列是否满足 GM(1,1) 建模条件（>=4 点且全为正）。"""
    return len(series) >= MIN_SAMPLES and all(x > 0 for x in series)


def predict(series: List[float], horizon: int) -> Tuple[List[float], float]:
    """
    GM(1,1) 向前预测。

    :param series: 历史正数序列（长度 >= 4）
    :param horizon: 预测步数
    :return: (预测序列, 平均相对误差)
    """
    x0 = np.asarray(series, dtype=float)
    n = len(x0)

    # 一次累加序列
    x1 = np.cumsum(x0)
    # 紧邻均值生成序列 z(k) = 0.5*x1(k) + 0.5*x1(k-1), k=2..n
    z1 = 0.5 * x1[1:] + 0.5 * x1[:-1]

    # 构造矩阵方程 Y = B * [a, b]^T
    y = x0[1:]
    b_mat = np.column_stack([-z1, np.ones(n - 1)])
    # 最小二乘求解
    params, *_ = np.linalg.lstsq(b_mat, y, rcond=None)
    a, b = float(params[0]), float(params[1])

    # a 过大说明序列震荡剧烈，灰色模型不适用
    if abs(a) >= 0.9:
        # 退化为末值保持
        return [float(x0[-1])] * horizon, 1.0

    def x1_hat(k: int) -> float:
        """白化方程解：累加序列第 k 期预测值（k 从 0 开始）。"""
        return (x0[0] - b / a) * np.exp(-a * k) + b / a

    # 累减还原
    result: List[float] = []
    for k in range(n, n + horizon):
        val = x1_hat(k) - x1_hat(k - 1)
        result.append(float(val))

    # ---- 拟合精度评估：平均相对误差 ----
    errors = []
    for k in range(1, n):
        pred_k = x1_hat(k) - x1_hat(k - 1)
        if abs(x0[k]) > 1e-9:
            errors.append(abs(pred_k - x0[k]) / abs(x0[k]))
    mae = float(np.mean(errors)) if errors else 0.0

    return result, mae
