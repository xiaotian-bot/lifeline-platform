-- ============================================================
-- 安塞区城市安全生命线管网AI智慧平台
-- TDengine 3.x 特征超级表初始化脚本（P6 Flink 实时链路）
-- 库：pipe_ts（由 02-tdengine-init.sql 创建）
-- 超级表：sensor_feature（滑动窗口统计特征）
-- 产出方：lifeline-flink-jobs FeatureWindowJob
--         （1min/5min/1h 滑动窗口，6 类指标 avg/peak/std/变化率）
--
-- 执行方式：docker-compose.yml 已将本脚本挂载至 TDengine 容器
--   /docker-entrypoint-initdb.d/05-tdengine-feature-stable.sql
--   容器首次初始化时自动执行；若容器已初始化过（数据卷已存在），
--   请手动执行：
--   docker exec -i lifeline-tdengine taos < deploy/sql/05-tdengine-feature-stable.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS pipe_ts WAL_LEVEL 1 BUFFER 256 PAGES 4096 KEEP 1825 DURATION 30;

USE pipe_ts;

CREATE STABLE IF NOT EXISTS sensor_feature (
  ts TIMESTAMP,
  metric_name NCHAR(32),
  avg_value FLOAT,
  peak_value FLOAT,
  std_value FLOAT,
  rate_of_change FLOAT,
  sample_count INT,
  window_start TIMESTAMP,
  window_end TIMESTAMP
) TAGS (
  device_id NCHAR(64),
  pipe_type NCHAR(32),
  sensor_type NCHAR(32),
  interval_type NCHAR(8)
);
