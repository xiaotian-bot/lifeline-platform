# -*- coding: utf-8 -*-
"""
Agent 调用日志：每次请求落 logs/agent-call.log（JSON 行）。

记录字段：时间 / 接口 / 入参摘要 / 耗时(ms) / provider / 状态。
"""
import json
import logging
import os
import threading
from datetime import datetime

from app import config

_LOGGER_NAME = "agent-call"
_lock = threading.Lock()
_inited = False


def _init_logger() -> logging.Logger:
    """初始化文件日志（幂等）。"""
    global _inited
    logger = logging.getLogger(_LOGGER_NAME)
    if _inited:
        return logger
    with _lock:
        if _inited:
            return logger
        os.makedirs(config.LOG_DIR, exist_ok=True)
        handler = logging.FileHandler(config.CALL_LOG_FILE, encoding="utf-8")
        handler.setFormatter(logging.Formatter("%(message)s"))
        logger.addHandler(handler)
        logger.setLevel(logging.INFO)
        logger.propagate = False
        _inited = True
    return logger


def log_call(endpoint: str, input_summary: dict, cost_ms: int,
             provider: str, status: str = "success") -> None:
    """
    写入一条调用日志（JSON 行）。

    :param endpoint:      接口路径，如 /api/v1/ai/agent/judge
    :param input_summary: 入参摘要（已脱敏/截断）
    :param cost_ms:       耗时（毫秒）
    :param provider:      LLM 供应商名称
    :param status:        success / error
    """
    record = {
        "time": datetime.now().isoformat(timespec="milliseconds"),
        "endpoint": endpoint,
        "inputSummary": input_summary,
        "costMs": cost_ms,
        "provider": provider,
        "status": status,
    }
    _init_logger().info(json.dumps(record, ensure_ascii=False))


def summarize_input(data: dict, max_len: int = 200) -> dict:
    """
    入参摘要：截断长文本字段，避免日志膨胀。

    :param data:    原始入参字典
    :param max_len: 单字段最大长度
    :return: 摘要字典
    """
    summary = {}
    for k, v in (data or {}).items():
        if isinstance(v, str) and len(v) > max_len:
            summary[k] = v[:max_len] + "..."
        else:
            summary[k] = v
    return summary
