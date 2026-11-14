# -*- coding: utf-8 -*-
"""
私有化部署大模型适配器（占位实现）。

面向后续本地化部署的国产大模型（OpenAI 兼容协议），
当前仅提供调用骨架：未配置有效地址时 available=False。
"""
import json
import urllib.request
from typing import Dict, Optional

from app import config
from app.llm.base import LlmAdapter


class LocalAdapter(LlmAdapter):
    """私有化大模型适配器。"""

    def __init__(self) -> None:
        self._url = config.LOCAL_LLM_URL
        self._model = config.LOCAL_LLM_MODEL

    @property
    def name(self) -> str:
        return "local"

    @property
    def available(self) -> bool:
        """未配置私有化服务地址时不可用（默认占位地址视为未启用）。"""
        return bool(self._url) and "localhost:8080" not in self._url

    def complete(self, prompt: str, context: Optional[Dict] = None) -> str:
        """按 OpenAI 兼容协议调用私有化模型。"""
        if not self.available:
            raise RuntimeError("私有化大模型适配器不可用：未配置有效 LOCAL_LLM_URL")

        payload = {
            "model": self._model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.2,
        }
        req = urllib.request.Request(
            url=self._url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=120) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        return data["choices"][0]["message"]["content"]
