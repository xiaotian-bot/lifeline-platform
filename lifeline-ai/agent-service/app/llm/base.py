# -*- coding: utf-8 -*-
"""
LLM 适配器抽象基类。

所有实现需提供：
    - name:      适配器名称（用于日志与监控）
    - available: 当前配置下是否可用
    - complete:  给定提示词与上下文，返回模型输出文本
"""
from abc import ABC, abstractmethod
from typing import Dict, Optional


class LlmAdapter(ABC):
    """大模型适配器抽象基类。"""

    @property
    @abstractmethod
    def name(self) -> str:
        """适配器名称。"""

    @property
    @abstractmethod
    def available(self) -> bool:
        """当前配置下是否可用。"""

    @abstractmethod
    def complete(self, prompt: str, context: Optional[Dict] = None) -> str:
        """
        执行一次补全调用。

        :param prompt:  提示词（含任务标记与结构化输出要求）
        :param context: 业务上下文（预警信息等），供适配器生成结构化结果
        :return: 模型输出文本（结构化任务约定为 JSON 字符串）
        """
