-- ============================================================
-- 安塞区城市安全生命线管网AI智慧平台
-- TDengine 3.x 初始化脚本
-- 库：pipe_ts（保留5年：KEEP 1825 天，分片 30 天）
-- 超级表：sensor_data（传感监测数据）
-- ============================================================

CREATE DATABASE IF NOT EXISTS pipe_ts WAL_LEVEL 1 BUFFER 256 PAGES 4096 KEEP 1825 DURATION 30;

USE pipe_ts;

CREATE STABLE IF NOT EXISTS sensor_data (
  ts TIMESTAMP,
  temperature FLOAT,
  humidity FLOAT,
  pressure FLOAT,
  flow_rate FLOAT,
  gas_concentration FLOAT,
  vibration FLOAT,
  pipe_status INT,
  corrosion FLOAT,
  leakage FLOAT
) TAGS (
  device_id NCHAR(64),
  pipe_type NCHAR(32),
  sensor_type NCHAR(32),
  area_id NCHAR(32)
);
