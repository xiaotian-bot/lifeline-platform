# -*- coding: utf-8 -*-
"""
pydantic 数据模型：研判 / 预案 / 问答 / 工作流的请求与响应结构。

字段命名与 Java 侧保持一致（camelCase alias），
请求同时兼容 camelCase 与 snake_case。
"""
from typing import Any, Dict, List, Optional, Union

from pydantic import BaseModel, ConfigDict, Field


# ---------------------------------------------------------------------------
# 请求模型
# ---------------------------------------------------------------------------

class JudgeRequest(BaseModel):
    """预警智能研判请求。"""

    model_config = ConfigDict(populate_by_name=True)

    warning_id: Optional[int] = Field(default=None, alias="warningId", description="预警事件ID")
    pipe_type: str = Field(default="", alias="pipeType", description="管网类型 GAS/WATER/HEATING/DRAINAGE")
    warning_level: Union[int, str] = Field(default=4, alias="warningLevel", description="预警等级 1-4")
    metric_type: str = Field(default="", alias="metricType", description="指标类型")
    trigger_value: Optional[float] = Field(default=None, alias="triggerValue", description="触发值")
    threshold: Optional[float] = Field(default=None, description="阈值")
    title: str = Field(default="", description="预警标题")
    description: str = Field(default="", description="预警描述")


class PlanRequest(JudgeRequest):
    """处置预案生成请求（研判字段 + 模板提示）。"""

    template_hint: Optional[str] = Field(default=None, alias="templateHint", description="预案模板提示，可空")


class ChatRequest(BaseModel):
    """知识问答请求。"""

    model_config = ConfigDict(populate_by_name=True)

    question: str = Field(..., min_length=1, description="用户问题")
    context: Optional[str] = Field(default=None, description="上下文补充，可空")


class WorkflowRequest(PlanRequest):
    """研判->预案 链式工作流请求。"""


# ---------------------------------------------------------------------------
# 响应模型
# ---------------------------------------------------------------------------

class JudgeResult(BaseModel):
    """结构化研判结果。"""

    model_config = ConfigDict(populate_by_name=True)

    root_cause: str = Field(alias="rootCause", description="根因分析")
    level_review: str = Field(alias="levelReview", description="等级复核建议")
    impact_scope: str = Field(alias="impactScope", description="影响范围")
    trend_prediction: str = Field(alias="trendPrediction", description="趋势预测")
    handle_priority: str = Field(alias="handlePriority", description="处置优先级 HIGH/MEDIUM/LOW")
    confidence: float = Field(ge=0, le=1, description="置信度")


class PlanTask(BaseModel):
    """预案任务项。"""

    task_name: str = Field(alias="taskName")
    task_type: str = Field(alias="taskType", description="任务类型 INSPECTION/OPERATION/REPAIR/MANAGEMENT")
    priority: str = Field(description="优先级 HIGH/MEDIUM/LOW")
    suggest_assignee: str = Field(alias="suggestAssignee", description="建议指派岗位")


class PlanResource(BaseModel):
    """预案资源项。"""

    resource_type: str = Field(alias="resourceType", description="资源类型 VEHICLE/EQUIPMENT/MATERIAL/PERSONNEL")
    resource_name: str = Field(alias="resourceName")
    quantity: int = Field(ge=1)


class PlanResult(BaseModel):
    """处置预案结果。"""

    model_config = ConfigDict(populate_by_name=True)

    plan_name: str = Field(alias="planName")
    tasks: List[PlanTask] = Field(default_factory=list)
    resources: List[PlanResource] = Field(default_factory=list)
    suggestion: str = Field(description="综合建议")


class ChatResult(BaseModel):
    """知识问答结果。"""

    answer: str
    references: List[str] = Field(default_factory=list, description="引用来源列表")


class WorkflowResult(BaseModel):
    """研判->预案 工作流合并结果。"""

    model_config = ConfigDict(populate_by_name=True)

    judge: JudgeResult = Field(description="研判结论")
    plan: PlanResult = Field(description="处置预案")
    cost_ms: int = Field(default=0, alias="costMs", description="工作流总耗时（毫秒）")


class ApiResult(BaseModel):
    """统一响应结构（与 Java 侧 Result<T> 对齐）。"""

    code: int = 200
    message: str = "success"
    data: Optional[Dict[str, Any]] = None
