# -*- coding: utf-8 -*-
"""
Agent 接口路由。

接口清单：
    POST /api/v1/ai/agent/judge     预警智能研判
    POST /api/v1/ai/agent/plan      处置预案生成
    POST /api/v1/ai/agent/chat      知识问答
    POST /api/v1/ai/agent/workflow  研判->预案链式工作流（LangGraph）

每个接口均：入参敏感词过滤 -> 业务执行 -> 调用日志落盘 -> 统一结构响应。
"""
import logging
import time

from fastapi import APIRouter

from app.llm.factory import get_adapter
from app.models.schemas import (
    ApiResult,
    ChatRequest,
    JudgeRequest,
    PlanRequest,
    WorkflowRequest,
)
from app.services import chat_service, judge_service, plan_service, workflow_service
from app.utils.call_logger import log_call, summarize_input
from app.utils.sensitive import filter_sensitive

logger = logging.getLogger("agent-service")

router = APIRouter(prefix="/api/v1/ai/agent", tags=["AI Agent 服务"])


def _sanitize_judge_fields(req: JudgeRequest) -> None:
    """对请求中的自由文本字段做敏感词过滤（原地修改）。"""
    req.title = filter_sensitive(req.title)
    req.description = filter_sensitive(req.description)


@router.post("/judge", response_model=ApiResult, summary="预警智能研判")
def judge(req: JudgeRequest) -> ApiResult:
    """对预警事件做结构化研判（根因/等级复核/影响范围/趋势/优先级）。"""
    start = time.perf_counter()
    _sanitize_judge_fields(req)
    try:
        result = judge_service.judge(req)
        data = result.model_dump(by_alias=True)
        status = "success"
        resp = ApiResult(code=200, message="success", data=data)
    except Exception as exc:  # noqa: BLE001 统一兜底
        logger.exception("研判失败")
        status = "error"
        resp = ApiResult(code=500, message=f"研判失败：{exc}", data=None)
    log_call("/api/v1/ai/agent/judge",
             summarize_input(req.model_dump(by_alias=True)),
             int((time.perf_counter() - start) * 1000),
             get_adapter().name, status)
    return resp


@router.post("/plan", response_model=ApiResult, summary="处置预案生成")
def plan(req: PlanRequest) -> ApiResult:
    """根据预警信息生成处置预案（任务分解 + 资源清单）。"""
    start = time.perf_counter()
    _sanitize_judge_fields(req)
    if req.template_hint:
        req.template_hint = filter_sensitive(req.template_hint)
    try:
        result = plan_service.plan(req)
        data = result.model_dump(by_alias=True)
        status = "success"
        resp = ApiResult(code=200, message="success", data=data)
    except Exception as exc:  # noqa: BLE001
        logger.exception("预案生成失败")
        status = "error"
        resp = ApiResult(code=500, message=f"预案生成失败：{exc}", data=None)
    log_call("/api/v1/ai/agent/plan",
             summarize_input(req.model_dump(by_alias=True)),
             int((time.perf_counter() - start) * 1000),
             get_adapter().name, status)
    return resp


@router.post("/chat", response_model=ApiResult, summary="知识问答")
def chat(req: ChatRequest) -> ApiResult:
    """基于内置知识库的智能问答。"""
    start = time.perf_counter()
    req.question = filter_sensitive(req.question)
    if req.context:
        req.context = filter_sensitive(req.context)
    try:
        result = chat_service.chat(req)
        data = result.model_dump(by_alias=True)
        status = "success"
        resp = ApiResult(code=200, message="success", data=data)
    except Exception as exc:  # noqa: BLE001
        logger.exception("问答失败")
        status = "error"
        resp = ApiResult(code=500, message=f"问答失败：{exc}", data=None)
    log_call("/api/v1/ai/agent/chat",
             summarize_input(req.model_dump(by_alias=True)),
             int((time.perf_counter() - start) * 1000),
             get_adapter().name, status)
    return resp


@router.post("/workflow", response_model=ApiResult, summary="研判->预案工作流")
def workflow(req: WorkflowRequest) -> ApiResult:
    """LangGraph 链式编排：研判结论自动喂给预案生成，端到端返回合并结果。"""
    start = time.perf_counter()
    _sanitize_judge_fields(req)
    if req.template_hint:
        req.template_hint = filter_sensitive(req.template_hint)
    try:
        result = workflow_service.run_workflow(req)
        data = result.model_dump(by_alias=True)
        status = "success"
        resp = ApiResult(code=200, message="success", data=data)
    except Exception as exc:  # noqa: BLE001
        logger.exception("工作流执行失败")
        status = "error"
        resp = ApiResult(code=500, message=f"工作流执行失败：{exc}", data=None)
    log_call("/api/v1/ai/agent/workflow",
             summarize_input(req.model_dump(by_alias=True)),
             int((time.perf_counter() - start) * 1000),
             get_adapter().name, status)
    return resp
