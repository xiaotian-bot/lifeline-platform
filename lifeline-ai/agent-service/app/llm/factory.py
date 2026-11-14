# -*- coding: utf-8 -*-
"""
LLM 适配器工厂：按配置选择并缓存适配器实例。

策略：
    - llm.provider=mock   -> MockLlmAdapter（默认，始终可用）
    - llm.provider=openai -> OpenAiAdapter（未配置 key 时自动降级 Mock 并告警）
    - llm.provider=local  -> LocalAdapter（未配置地址时自动降级 Mock 并告警）
"""
import logging
from typing import Optional

from app import config
from app.llm.base import LlmAdapter
from app.llm.local_adapter import LocalAdapter
from app.llm.mock_adapter import MockLlmAdapter
from app.llm.openai_adapter import OpenAiAdapter

logger = logging.getLogger("agent-service")

_ADAPTER: Optional[LlmAdapter] = None


def get_adapter() -> LlmAdapter:
    """获取全局唯一的 LLM 适配器实例（懒加载 + 降级兜底）。"""
    global _ADAPTER
    if _ADAPTER is not None:
        return _ADAPTER

    provider = config.LLM_PROVIDER
    if provider == "openai":
        adapter = OpenAiAdapter()
    elif provider == "local":
        adapter = LocalAdapter()
    else:
        adapter = MockLlmAdapter()

    # 目标适配器不可用时降级为 Mock，保证服务可用
    if not adapter.available:
        logger.warning(
            "LLM provider=%s 不可用（缺少配置），已降级为 MockLlmAdapter", provider
        )
        adapter = MockLlmAdapter()

    logger.info("LLM 适配器已初始化：provider=%s", adapter.name)
    _ADAPTER = adapter
    return _ADAPTER
