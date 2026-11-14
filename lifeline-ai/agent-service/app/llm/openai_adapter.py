# -*- coding: utf-8 -*-
"""
OpenAI 商用 API 适配器（配置占位实现）。

- 未配置 OPENAI_API_KEY 时 available=False，工厂将拒绝使用该适配器；
- 使用标准库 urllib 调用 chat/completions，避免引入额外依赖。
"""
import json
import urllib.request
from typing import Dict, Optional

from app import config
from app.llm.base import LlmAdapter


class OpenAiAdapter(LlmAdapter):
    """OpenAI 兼容接口适配器。"""

    def __init__(self) -> None:
        self._api_key = config.OPENAI_API_KEY
        self._base_url = config.OPENAI_BASE_URL.rstrip("/")
        self._model = config.OPENAI_MODEL

    @property
    def name(self) -> str:
        return "openai"

    @property
    def available(self) -> bool:
        """未配置 API Key 时不可用。"""
        return bool(self._api_key)

    def complete(self, prompt: str, context: Optional[Dict] = None) -> str:
        """调用 chat/completions 接口。"""
        if not self.available:
            raise RuntimeError("OpenAI 适配器不可用：未配置 OPENAI_API_KEY")

        payload = {
            "model": self._model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.2,
        }
        req = urllib.request.Request(
            url=f"{self._base_url}/chat/completions",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self._api_key}",
            },
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        return data["choices"][0]["message"]["content"]
