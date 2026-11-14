-- ============================================================
-- 06-operation-order-alter.sql
-- 运维工单表（operation_order）增量补列脚本（operation-service P9 需求）
--
-- 背景：01-postgres-init.sql 中的 operation_order 表未包含标题/区域/
--       来源字段，本脚本以幂等方式（IF NOT EXISTS）补齐。
-- 说明：全部语句可重复执行，已存在的列不会被覆盖或报错。
-- ============================================================

-- 工单标题
ALTER TABLE operation_order ADD COLUMN IF NOT EXISTS title varchar(255);
COMMENT ON COLUMN operation_order.title IS '工单标题';

-- 所属区域ID
ALTER TABLE operation_order ADD COLUMN IF NOT EXISTS area_id bigint;
COMMENT ON COLUMN operation_order.area_id IS '所属区域ID';

-- 工单来源：WARNING 预警转派 / PLAN 预案派生 / MANUAL 人工创建
ALTER TABLE operation_order ADD COLUMN IF NOT EXISTS source varchar(16);
COMMENT ON COLUMN operation_order.source IS '工单来源：WARNING 预警转派 / PLAN 预案派生 / MANUAL 人工创建';

-- 查询常用索引：区域/来源（幂等，uk_operation_order_code 已在 01 号脚本创建）
CREATE INDEX IF NOT EXISTS idx_oo_area_id ON operation_order (area_id);
CREATE INDEX IF NOT EXISTS idx_oo_source ON operation_order (source);
