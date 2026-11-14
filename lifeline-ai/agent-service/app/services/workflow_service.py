# -*- coding: utf-8 -*-
"""
研判 -> 预案 链式工作流（LangGraph StateGraph 编排）。

拓扑：START -> judge -> plan -> END
    - judge 节点：调用研判服务，产出结构化研判结论；
    - plan 节点：将研判结论注入上下文，生成衔接的处置预案；
    - 状态在节点间通过 TypedDict 传递。
"""
import time
from typing import Any, Dict, Optional, TypedDict

from langgraph.graph import END, START, StateGraph

from app.models.schemas import JudgeResult, PlanRequest, PlanResult, WorkflowResult
from app.services import judge_service, plan_service


class WorkflowState(TypedDict, total=False):
    """工作流状态：请求 + 各节点产出。"""

    request: PlanRequest               # 原始请求
    judge: Optional[Dict[str, Any]]    # 研判结论（camelCase dict）
    plan: Optional[Dict[str, Any]]     # 处置预案（camelCase dict）


def _judge_node(state: WorkflowState) -> WorkflowState:
    """研判节点：执行智能研判并写入状态。"""
    req = state["request"]
    result: JudgeResult = judge_service.judge(req)
    state["judge"] = result.model_dump(by_alias=True)
    return state


def _plan_node(state: WorkflowState) -> WorkflowState:
    """预案节点：基于研判结论生成处置预案。"""
    req = state["request"]
    result: PlanResult = plan_service.plan_with_judge(req, state.get("judge") or {})
    state["plan"] = result.model_dump(by_alias=True)
    return state


def _build_graph():
    """构建并编译 LangGraph 工作流图。"""
    graph = StateGraph(WorkflowState)
    graph.add_node("judge", _judge_node)
    graph.add_node("plan", _plan_node)
    graph.add_edge(START, "judge")
    graph.add_edge("judge", "plan")
    graph.add_edge("plan", END)
    return graph.compile()


# 模块级单例（编译一次复用）
_COMPILED = _build_graph()


def run_workflow(req: PlanRequest) -> WorkflowResult:
    """
    执行研判->预案链式工作流。

    :param req: 工作流请求（同预案请求）
    :return: 合并结构化结果（judge + plan + 总耗时）
    """
    start = time.perf_counter()
    final_state = _COMPILED.invoke({"request": req, "judge": None, "plan": None})

    judge_dict = final_state.get("judge") or {}
    plan_dict = final_state.get("plan") or {}
    cost_ms = int((time.perf_counter() - start) * 1000)

    return WorkflowResult(
        judge=JudgeResult.model_validate(judge_dict),
        plan=PlanResult.model_validate(plan_dict),
        cost_ms=cost_ms,
    )
