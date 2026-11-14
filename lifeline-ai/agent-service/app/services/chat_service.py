# -*- coding: utf-8 -*-
"""
知识问答服务：基于静态知识库关键词检索 + LLM 适配器组织回答。
"""
from app.llm.factory import get_adapter
from app.models.schemas import ChatRequest, ChatResult
from app.utils.json_parser import extract_json
from app.utils.sensitive import filter_dict_strings

CHAT_PROMPT = """[TASK:CHAT]
你是城市生命线管网运维知识助手，请结合知识库内容回答用户问题。
要求输出 JSON，字段：answer（回答）、references（引用来源数组）。

用户问题：{question}
补充上下文：{context}
"""


def chat(req: ChatRequest) -> ChatResult:
    """
    知识问答。

    :param req: 问答请求（question 必填）
    :return: 回答 + 引用来源
    """
    adapter = get_adapter()
    prompt = CHAT_PROMPT.format(question=req.question, context=req.context or "无")
    raw = adapter.complete(
        prompt,
        context={"question": req.question, "context": req.context or ""},
    )
    data = extract_json(raw)
    data = filter_dict_strings(data)
    return ChatResult.model_validate(data)
