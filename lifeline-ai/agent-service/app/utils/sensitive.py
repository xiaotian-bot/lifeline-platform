# -*- coding: utf-8 -*-
"""
敏感词简单过滤。

对输入与输出文本做关键词替换，避免不当内容进入/流出平台。
"""
from typing import List

# 敏感词表（演示用，实际部署建议接入审核服务或更完整词库）
SENSITIVE_WORDS: List[str] = [
    "赌博", "色情", "吸毒", "贩毒", "邪教", "恐怖袭击",
    "自杀", "自残", "枪支", "弹药", "洗钱",
]

_MASK = "***"


def contains_sensitive(text: str) -> bool:
    """判断文本是否包含敏感词。"""
    return any(w in text for w in SENSITIVE_WORDS)


def filter_sensitive(text: str) -> str:
    """将文本中的敏感词替换为 ***。"""
    if not text:
        return text
    for word in SENSITIVE_WORDS:
        text = text.replace(word, _MASK)
    return text


def filter_dict_strings(data: dict) -> dict:
    """对字典中的字符串值递归做敏感词过滤。"""
    if isinstance(data, dict):
        return {k: filter_dict_strings(v) for k, v in data.items()}
    if isinstance(data, list):
        return [filter_dict_strings(v) for v in data]
    if isinstance(data, str):
        return filter_sensitive(data)
    return data
