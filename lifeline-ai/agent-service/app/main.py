# -*- coding: utf-8 -*-
"""
agent-service 应用入口。

启动方式：
    uvicorn app.main:app --host 0.0.0.0 --port 8001
"""
import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.llm.factory import get_adapter
from app.routers import agent_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger("agent-service")

app = FastAPI(
    title="lifeline agent-service",
    description="安塞区城市安全生命线管网AI智慧平台 —— 预警智能研判与处置预案 Agent 服务",
    version="1.0.0",
)

app.include_router(agent_router.router)


@app.on_event("startup")
def _startup() -> None:
    """启动时初始化 LLM 适配器与知识库索引。"""
    adapter = get_adapter()
    logger.info("agent-service 启动完成，LLM provider=%s", adapter.name)


# ---------------------------------------------------------------------------
# 全局异常兜底：统一返回结构化错误
# ---------------------------------------------------------------------------
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """HTTP 异常包装为统一结构。"""
    return JSONResponse(
        status_code=exc.status_code,
        content={"code": exc.status_code, "message": str(exc.detail), "data": None},
    )


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """请求体参数校验失败：返回 400 结构化错误。"""
    first = exc.errors()[0] if exc.errors() else {}
    loc = ".".join(str(x) for x in first.get("loc", []) if x != "body")
    msg = f"参数校验失败：{loc} {first.get('msg', '')}"
    return JSONResponse(status_code=400, content={"code": 400, "message": msg, "data": None})


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """未捕获异常兜底。"""
    logger.exception("未处理异常：%s", exc)
    return JSONResponse(
        status_code=500,
        content={"code": 500, "message": f"服务内部错误：{exc}", "data": None},
    )


@app.get("/", tags=["健康检查"])
def root() -> dict:
    """服务存活探针（附带当前 LLM provider）。"""
    return {
        "service": "lifeline-agent-service",
        "status": "UP",
        "llmProvider": get_adapter().name,
    }


@app.get("/actuator/health", tags=["健康检查"])
def health() -> dict:
    """兼容 Spring 风格的健康检查端点。"""
    return {"status": "UP"}
