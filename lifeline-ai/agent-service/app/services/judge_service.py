# -*- coding: utf-8 -*-
"""
预警智能研判服务。

流程：构造带任务标记的提示词 -> LLM 适配器补全 -> 解析 JSON
-> pydantic 结构校验 -> 敏感词过滤。
"""
import json

from app.llm.factory import get_adapter
from app.models.schemas import JudgeRequest, JudgeResult
from app.utils.json_parser import extract_json
from app.utils.sensitive import filter_dict_strings

JUDGE_PROMPT = """[TASK:JUDGE]
你是城市生命线管网安全专家，请根据以下预警信息进行结构化研判。
要求输出 JSON，字段：rootCause（根因分析）、levelReview（等级复核建议）、
impactScope（影响范围）、trendPrediction（趋势预测）、
handlePriority（HIGH/MEDIUM/LOW）、confidence（0-1）。

预警信息：
{context}
"""


def build_judge_prompt(req: JudgeRequest) -> str:
    """构造研判提示词。"""
    ctx = json.dumps({
        "warningId": req.warning_id,
        "pipeType": req.pipe_type,
        "warningLevel": req.warning_level,
        "metricType": req.metric_type,
        "triggerValue": req.trigger_value,
        "threshold": req.threshold,
        "title": req.title,
        "description": req.description,
    }, ensure_ascii=False, indent=2)
    return JUDGE_PROMPT.format(context=ctx)


def judge(req: JudgeRequest) -> JudgeResult:
    """
    执行智能研判。

    :param req: 研判请求
    :return: 结构化研判结果
    """
    adapter = get_adapter()
    context = req.model_dump(by_alias=True)
    raw = adapter.complete(build_judge_prompt(req), context=context)

    data = extract_json(raw)
    data = filter_dict_strings(data)
    # pydantic 结构校验（兼容 camelCase 输入）
    return JudgeResult.model_validate(data)
