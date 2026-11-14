#!/bin/bash
# ============================================================
# 安塞区城市安全生命线管网AI智慧平台 - Kafka Topic 初始化脚本
# 用法：
#   容器外执行（需本机安装 kafka 客户端）：./init-kafka-topics.sh [bootstrap-server]
#   容器内执行（推荐）：
#     docker exec lifeline-kafka bash /opt/init-kafka-topics.sh
#   或将本文件挂载进 kafka 容器后执行
# 依赖：kafka-topics 位于 PATH（confluentinc/cp-kafka 镜像）
# ============================================================
set -e

BOOTSTRAP_SERVER="${1:-localhost:9092}"
RETENTION_MS=604800   # 保留策略：7 天

# Topic 定义：名称 分区数 副本因子
declare -a TOPICS=(
  "pipe_sensor_raw 16 1"       # 原始传感数据
  "pipe_sensor_clean 16 1"     # 清洗后标准数据
  "pipe_video_event 4 1"       # 视频识别事件
  "pipe_warning_event 4 1"     # 预警事件
  "pipe_order_event 4 1"       # 运维工单事件
  "pipe_disposal_event 4 1"    # 处置进度事件
  "pipe_evidence_request 4 1"  # 证据调取请求
)

for entry in "${TOPICS[@]}"; do
  read -r topic partitions replicas <<< "$entry"
  echo ">>> 创建/校验 Topic: ${topic} (partitions=${partitions}, replicas=${replicas}, retention.ms=${RETENTION_MS})"
  kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor "${replicas}" \
    --config retention.ms="${RETENTION_MS}"
done

echo ">>> 所有 Topic 初始化完成，当前列表："
kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --list
