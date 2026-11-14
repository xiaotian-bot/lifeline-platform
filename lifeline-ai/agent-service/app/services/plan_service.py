# -*- coding: utf-8 -*-
"""
处置预案生成服务。

支持两种调用方式：
    1. 独立接口：由预警信息直接生成预案（plan(req)）；
    2. 工作流节点：接收研判结论上下文，生成衔接研判的预案
       （plan_with_judge(req, judge_dict)，供 LangGraph 编排调用）。
"""
import json
from typing import Dict, Optional

from app.llm.factory import get_adapter
from app.models.schemas import PlanRequest, PlanResult
from app.utils.json_parser import extract_json
from app.utils.sensitive import filter_dict_strings

PLAN_PROMPT = """[TASK:PLAN]
你是城市生命线管网应急处置专家，请根据预警信息与研判结论生成结构化处置预案。
要求输出 JSON，字段：planName（预案名称）、
tasks（数组，元素含 taskName/taskType/priority/suggestAssignee）、
resources（数组，元素含 resourceType/resourceName/quantity）、suggestion（综合建议）。

预警与研判信息：
{context}
"""


def _build_prompt(req: PlanRequest, judge_dict: Optional[Dict] = None) -> str:
    """构造预案提示词（可附带研判结论）。"""
    ctx = {
        "warningId": req.warning_id,
        "pipeType": req.pipe_type,
        "warningLevel": req.warning_level,
        "metricType": req.metric_type,
        "triggerValue": req.trigger_value,
        "threshold": req.threshold,
        "title": req.title,
        "description": req.description,
        "templateHint": req.template_hint,
    }
    if judge_dict:
        ctx["judgeResult"] = judge_dict
    return PLAN_PROMPT.format(
        context=json.dumps(ctx, ensure_ascii=False, indent=2))


def _do_plan(req: PlanRequest, judge_dict: Optional[Dict]) -> PlanResult:
    """预案生成公共逻辑：调用 LLM -> 解析 -> 校验 -> 过滤。"""
    adapter = get_adapter()
    context = req.model_dump(by_alias=True)
    if judge_dict:
        context["judgeResult"] = judge_dict
    raw = adapter.complete(_build_prompt(req, judge_dict), context=context)

    data = extract_json(raw)
    data = filter_dict_strings(data)
    return PlanResult.model_validate(data)


def plan(req: PlanRequest) -> PlanResult:
    """独立预案生成接口逻辑。"""
    return _do_plan(req, None)


def plan_with_judge(req: PlanRequest, judge_dict: Dict) -> PlanResult:
    """工作流节点逻辑：基于研判结论生成预案。"""
    return _do_plan(req, judge_dict)
