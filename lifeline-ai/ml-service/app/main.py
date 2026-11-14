# -*- coding: utf-8 -*-
"""
ml-service 应用入口。

启动方式：
    uvicorn app.main:app --host 0.0.0.0 --port 8000
"""
import logging
import time

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.routers import ai_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
)
logger = logging.getLogger("ml-service")

app = FastAPI(
    title="lifeline ml-service",
    description="安塞区城市安全生命线管网AI智慧平台 —— 时序异常检测与健康预报算法服务",
    version="1.0.0",
)

app.include_router(ai_router.router)


# ---------------------------------------------------------------------------
# 请求耗时日志中间件
# ---------------------------------------------------------------------------
@app.middleware("http")
async def cost_logging_middleware(request: Request, call_next):
    """记录每个请求的方法、路径与耗时（毫秒）。"""
    start = time.perf_counter()
    response = await call_next(request)
    cost_ms = int((time.perf_counter() - start) * 1000)
    response.headers["X-Cost-Ms"] = str(cost_ms)
    logger.info("%s %s -> %s (%sms)", request.method, request.url.path,
                response.status_code, cost_ms)
    return response


# ---------------------------------------------------------------------------
# 全局异常兜底：统一返回结构化错误 {"code": 5xx/4xx, "message": "...", "data": null}
# ---------------------------------------------------------------------------
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """HTTP 异常（如 400 参数错误）：同样包装为统一结构。"""
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
    """未捕获异常兜底：返回 500 结构化错误。"""
    logger.exception("未处理异常：%s", exc)
    return JSONResponse(
        status_code=500,
        content={"code": 500, "message": f"服务内部错误：{exc}", "data": None},
    )


@app.get("/", tags=["健康检查"])
def root() -> dict:
    """服务存活探针。"""
    return {"service": "lifeline-ml-service", "status": "UP"}


@app.get("/actuator/health", tags=["健康检查"])
def health() -> dict:
    """兼容 Spring 风格的健康检查端点。"""
    return {"status": "UP"}
