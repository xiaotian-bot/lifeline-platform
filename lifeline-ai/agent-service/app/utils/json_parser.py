# -*- coding: utf-8 -*-
"""LLM 输出 JSON 解析工具。"""
import json
import re
from typing import Dict


def extract_json(text: str) -> Dict:
    """
    从模型输出中提取 JSON 对象。

    容错处理：
        - 去除 markdown ```json ... ``` 代码块包裹；
        - 截取首个 { 到末个 } 的子串。

    :param text: 模型原始输出
    :return: 解析后的字典
    :raises ValueError: 无法解析为 JSON 时抛出
    """
    text = (text or "").strip()
    m = re.search(r"```(?:json)?\s*(\{.*\})\s*```", text, re.S)
    if m:
        text = m.group(1)
    else:
        start, end = text.find("{"), text.rfind("}")
        if start >= 0 and end > start:
            text = text[start:end + 1]
    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        raise ValueError(f"LLM 输出无法解析为 JSON：{e}") from e
