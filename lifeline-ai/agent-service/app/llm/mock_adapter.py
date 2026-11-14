# -*- coding: utf-8 -*-
"""
MockLlmAdapter —— 默认激活的模拟大模型适配器。

关键决策（Mock 先行）：在无外部 LLM API 的环境下，
基于「规则模板 + 内置知识库关键词检索」生成结构化 JSON，
保证研判 / 预案 / 问答 / 工作流四条链路端到端可跑通。

任务识别：通过 prompt 中的任务标记分发：
    [TASK:JUDGE]  预警研判
    [TASK:PLAN]   处置预案
    [TASK:CHAT]   知识问答
"""
import json
from typing import Dict, Optional

from app.knowledge.retriever import retrieve
from app.llm.base import LlmAdapter

PIPE_TYPE_CN = {"GAS": "燃气", "WATER": "供水", "HEATING": "供热", "DRAINAGE": "排水"}
METRIC_CN = {
    "pressure": "压力", "temperature": "温度", "flow": "流量",
    "vibration": "振动", "corrosion": "腐蚀", "leakage": "泄漏",
}
LEVEL_CN = {1: "一级（特别重大）", 2: "二级（重大）", 3: "三级（较大）", 4: "四级（一般）"}

# 各指标异常的根因研判规则（按 metricType 匹配）
ROOT_CAUSE_RULES = {
    "pressure": "管道压力异常下降，疑似存在泄漏点或上游供气/供水压力波动，"
                "不排除接口松动、管体破损的可能",
    "temperature": "温度指标异常，疑似供热管网热损失增大或伴热/保温设施失效",
    "flow": "流量指标异常，疑似管段堵塞、阀门误动作或计量设备故障",
    "vibration": "振动指标超标，疑似管道支撑松动、水锤冲击或外部施工扰动",
    "corrosion": "腐蚀指标持续上升，管体壁厚减薄风险增大，存在劣化失效隐患",
    "leakage": "泄漏监测指标报警，疑似管体或接口发生泄漏",
}


def _ratio(ctx: Dict) -> float:
    """计算触发值相对阈值的偏离比例（绝对值）。"""
    try:
        tv = float(ctx.get("triggerValue") or 0)
        th = float(ctx.get("threshold") or 0)
        if abs(th) < 1e-9:
            return 0.0
        return abs(tv - th) / abs(th)
    except (TypeError, ValueError):
        return 0.0


class MockLlmAdapter(LlmAdapter):
    """规则模板 + 知识库检索的模拟大模型适配器。"""

    @property
    def name(self) -> str:
        return "mock"

    @property
    def available(self) -> bool:
        """Mock 模式始终可用。"""
        return True

    def complete(self, prompt: str, context: Optional[Dict] = None) -> str:
        """根据任务标记分发到对应规则模板，返回 JSON 字符串。"""
        ctx = context or {}
        if "[TASK:JUDGE]" in prompt:
            return json.dumps(self._judge(ctx), ensure_ascii=False)
        if "[TASK:PLAN]" in prompt:
            return json.dumps(self._plan(ctx), ensure_ascii=False)
        if "[TASK:CHAT]" in prompt:
            return json.dumps(self._chat(ctx), ensure_ascii=False)
        # 未知任务：返回兜底说明
        return json.dumps({"answer": "Mock 模式暂不支持该任务类型", "references": []},
                          ensure_ascii=False)

    # ------------------------------------------------------------------
    # 研判
    # ------------------------------------------------------------------
    def _judge(self, ctx: Dict) -> Dict:
        """规则研判：根因 / 等级复核 / 影响范围 / 趋势 / 处置优先级。"""
        metric = str(ctx.get("metricType") or "")
        pipe = str(ctx.get("pipeType") or "").upper()
        level = ctx.get("warningLevel")
        pipe_cn = PIPE_TYPE_CN.get(pipe, pipe or "未知")
        metric_cn = METRIC_CN.get(metric, metric or "未知")
        ratio = _ratio(ctx)

        root_cause = ROOT_CAUSE_RULES.get(
            metric, f"{pipe_cn}管网{metric_cn}指标异常，原因待现场核实")

        # ---- 等级复核：偏离幅度大建议升级，幅度小建议降级 ----
        try:
            level_int = int(level)
        except (TypeError, ValueError):
            level_int = 4
        if ratio >= 0.5 and level_int > 1:
            level_review = (f"触发值偏离阈值 {ratio:.0%}，幅度较大，"
                            f"建议由{LEVEL_CN.get(level_int, str(level))}上调一级处置")
        elif ratio < 0.1 and level_int < 4:
            level_review = (f"触发值偏离阈值仅 {ratio:.0%}，幅度较小，"
                            f"可考虑由{LEVEL_CN.get(level_int, str(level))}下调一级或维持观察")
        else:
            level_review = f"偏离幅度 {ratio:.0%}，维持当前{LEVEL_CN.get(level_int, str(level))}等级处置"

        impact_scope = (
            f"影响范围为报警点位所在的{pipe_cn}管段及其下游用户，"
            f"若为泄漏类事件可能波及相邻道路与地下空间，需关注次生风险"
        )
        trend_prediction = (
            f"若不及时处置，{metric_cn}指标大概率继续恶化，"
            f"建议在 2 小时内复核趋势并启动相应等级响应"
        )

        # ---- 处置优先级 ----
        if level_int <= 2 or ratio >= 0.5:
            priority = "HIGH"
        elif level_int == 3:
            priority = "MEDIUM"
        else:
            priority = "LOW"

        confidence = min(0.7 + ratio * 0.3, 0.95)
        return {
            "rootCause": root_cause,
            "levelReview": level_review,
            "impactScope": impact_scope,
            "trendPrediction": trend_prediction,
            "handlePriority": priority,
            "confidence": round(confidence, 4),
        }

    # ------------------------------------------------------------------
    # 预案
    # ------------------------------------------------------------------
    def _plan(self, ctx: Dict) -> Dict:
        """规则生成处置预案：任务分解 + 资源清单。"""
        pipe = str(ctx.get("pipeType") or "").upper()
        metric = str(ctx.get("metricType") or "")
        hint = str(ctx.get("templateHint") or "").strip()
        pipe_cn = PIPE_TYPE_CN.get(pipe, pipe or "管网")
        metric_cn = METRIC_CN.get(metric, metric or "异常")
        # 根因优先取工作流上游研判结论，其次取直接入参
        judge_ctx = ctx.get("judgeResult") or {}
        root_cause = str(ctx.get("rootCause") or judge_ctx.get("rootCause")
                         or f"{metric_cn}指标异常")

        plan_name = f"{pipe_cn}管网{metric_cn}异常处置预案"
        if hint:
            plan_name += f"（{hint}）"

        tasks = [
            {"taskName": "预警核实与点位确认", "taskType": "INSPECTION",
             "priority": "HIGH", "suggestAssignee": "监测值班员"},
            {"taskName": "现场巡查与漏点/隐患定位", "taskType": "INSPECTION",
             "priority": "HIGH", "suggestAssignee": "管网巡检组"},
            {"taskName": "上下游阀门调控与隔离", "taskType": "OPERATION",
             "priority": "HIGH", "suggestAssignee": "运行调度组"},
            {"taskName": "应急抢修与缺陷处置", "taskType": "REPAIR",
             "priority": "HIGH", "suggestAssignee": "应急抢修队"},
            {"taskName": "复测验收与恢复运行", "taskType": "OPERATION",
             "priority": "MEDIUM", "suggestAssignee": "运行调度组"},
            {"taskName": "事件归档与复盘分析", "taskType": "MANAGEMENT",
             "priority": "LOW", "suggestAssignee": "安全管理员"},
        ]

        resources = [
            {"resourceType": "VEHICLE", "resourceName": "应急抢修车", "quantity": 1},
            {"resourceType": "EQUIPMENT", "resourceName": f"{pipe_cn}管线探测仪/检漏仪", "quantity": 2},
            {"resourceType": "MATERIAL", "resourceName": "阀门及管件备品", "quantity": 1},
            {"resourceType": "PERSONNEL", "resourceName": "抢修作业人员", "quantity": 6},
        ]

        suggestion = (
            f"研判结论：{root_cause}。建议优先执行阀门隔离与现场定位，"
            f"处置过程持续监测{metric_cn}指标变化，恢复运行前完成复测验收；"
            f"同步做好下游用户告知与舆情跟踪。"
        )
        return {
            "planName": plan_name,
            "tasks": tasks,
            "resources": resources,
            "suggestion": suggestion,
        }

    # ------------------------------------------------------------------
    # 问答
    # ------------------------------------------------------------------
    def _chat(self, ctx: Dict) -> Dict:
        """知识问答：关键词检索知识库后组织回答。"""
        question = str(ctx.get("question") or "")
        snippets = retrieve(question, top_k=3)

        if not snippets:
            return {
                "answer": "抱歉，内置知识库暂未收录该问题，建议联系平台运维或查阅专项预案文档。",
                "references": [],
            }

        body = "；".join(s["summary"] for s in snippets)
        answer = f"依据内置知识库：{body}。（以上为关键词检索摘要，详细规程请查阅对应文档）"
        return {
            "answer": answer,
            "references": [f"{s['source']}#{s['title']}" for s in snippets],
        }
