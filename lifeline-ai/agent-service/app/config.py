# -*- coding: utf-8 -*-
"""
全局配置。

LLM Provider 通过环境变量 LLM_PROVIDER 切换：
    mock   —— 默认激活，规则模板 + 知识库关键词检索，保证端到端可跑
    openai —— 商用 API（需 OPENAI_API_KEY，未配置时标记不可用）
    local  —— 私有化部署大模型（占位实现）
"""
import os


def _env(key: str, default: str = "") -> str:
    """读取环境变量（去除首尾空白）。"""
    return os.getenv(key, default).strip()


# LLM 供应商：mock | openai | local
LLM_PROVIDER = _env("LLM_PROVIDER", "mock").lower()

# ---- OpenAI 商用 API 配置（占位，未配置 key 时适配器不可用）----
OPENAI_API_KEY = _env("OPENAI_API_KEY")
OPENAI_BASE_URL = _env("OPENAI_BASE_URL", "https://api.openai.com/v1")
OPENAI_MODEL = _env("OPENAI_MODEL", "gpt-4o")

# ---- 私有化大模型配置（占位）----
LOCAL_LLM_URL = _env("LOCAL_LLM_URL", "http://localhost:8080/v1/chat/completions")
LOCAL_LLM_MODEL = _env("LOCAL_LLM_MODEL", "chatglm3-6b")

# ---- 知识库目录（相对本文件的上上级目录）----
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
KNOWLEDGE_BASE_DIR = os.path.join(BASE_DIR, "knowledge_base")

# ---- 调用日志 ----
LOG_DIR = os.path.join(BASE_DIR, "logs")
CALL_LOG_FILE = os.path.join(LOG_DIR, "agent-call.log")
