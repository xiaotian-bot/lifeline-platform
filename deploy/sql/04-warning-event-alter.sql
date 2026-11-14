-- ============================================================
-- 04-warning-event-alter.sql
-- 预警事件表（warning_event）增量补列脚本（warning-service P7 需求）
--
-- 背景：01-postgres-init.sql 中的 warning_event 表未包含来源/区域/标题/
--       指标/触发值/阈值/责任人等字段，本脚本以幂等方式（IF NOT EXISTS）补齐。
-- 说明：全部语句可重复执行，已存在的列不会被覆盖或报错。
-- ============================================================

-- 预警来源：RULE 规则引擎 / AI 模型 / MANUAL 人工
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS source varchar(16);
COMMENT ON COLUMN warning_event.source IS '预警来源：RULE 规则引擎 / AI 模型 / MANUAL 人工';

-- 所属区域ID
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS area_id bigint;
COMMENT ON COLUMN warning_event.area_id IS '所属区域ID';

-- 预警标题
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS title varchar(255);
COMMENT ON COLUMN warning_event.title IS '预警标题';

-- 指标类型：pressure/temperature/flow/vibration/corrosion/leakage
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS metric_type varchar(32);
COMMENT ON COLUMN warning_event.metric_type IS '指标类型：pressure/temperature/flow/vibration/corrosion/leakage';

-- 触发值（命中规则时的实际监测值）
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS trigger_value numeric(16,4);
COMMENT ON COLUMN warning_event.trigger_value IS '触发值（命中规则时的实际监测值）';

-- 阈值（命中规则的阈值）
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS threshold numeric(16,4);
COMMENT ON COLUMN warning_event.threshold IS '阈值（命中规则的阈值）';

-- 责任人ID（处置指派）
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS assignee_id bigint;
COMMENT ON COLUMN warning_event.assignee_id IS '责任人ID（处置指派）';

-- 查询常用索引：设备/区域/状态（幂等唯一索引 uk_warning_event_code 已在 01 号脚本创建）
CREATE INDEX IF NOT EXISTS idx_we_device_id ON warning_event (device_id);
CREATE INDEX IF NOT EXISTS idx_we_pipe_id ON warning_event (pipe_id);
CREATE INDEX IF NOT EXISTS idx_we_area_id ON warning_event (area_id);
CREATE INDEX IF NOT EXISTS idx_we_status ON warning_event (status);
