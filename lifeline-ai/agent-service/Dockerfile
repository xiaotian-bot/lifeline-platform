# lifeline agent-service 镜像（供 docker-compose 挂载使用）
FROM python:3.11-slim

WORKDIR /app

# 先装依赖利用缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY app ./app
COPY knowledge_base ./knowledge_base

# Mock 为默认供应商；部署时可通过 -e LLM_PROVIDER=openai|local 切换
ENV LLM_PROVIDER=mock

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
