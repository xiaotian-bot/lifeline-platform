# lifeline ml-service 镜像（供 docker-compose 挂载使用）
FROM python:3.11-slim

WORKDIR /app

# 先装依赖利用缓存
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY app ./app

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
