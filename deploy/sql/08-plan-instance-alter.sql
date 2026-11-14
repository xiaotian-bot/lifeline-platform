-- ============================================================
-- 08-plan-instance-alter.sql
-- 预案实例表（plan_instance）增量补列脚本（plan-service P9-2 需求）
--
-- 背景：01-postgres-init.sql 中的 plan_instance 表未包含预案名称 /
--       生成来源 / 综合处置建议 / 三级联动上报级别列，而冻结契约
--       PlanAgentResultDTO（planName / suggestion）落库、AI / TEMPLATE
--       来源区分及三级联动上报需要，本脚本以幂等方式（IF NOT EXISTS）补齐。
-- 说明：全部语句可重复执行，已存在的列不会被覆盖或报错。
-- ============================================================

-- 预案名称（AI 结果的 planName，模板降级时取模板名称）
ALTER TABLE plan_instance ADD COLUMN IF NOT EXISTS plan_name varchar(128);
COMMENT ON COLUMN plan_instance.plan_name IS '预案名称（AI 结果的 planName 或模板名）';

-- 实例来源：AI 智能生成 / TEMPLATE 模板实例化
ALTER TABLE plan_instance ADD COLUMN IF NOT EXISTS source varchar(16) DEFAULT 'TEMPLATE';
COMMENT ON COLUMN plan_instance.source IS '实例来源：AI 智能生成 / TEMPLATE 模板实例化';

-- 综合处置建议（AI 结果的 suggestion，模板降级时取模板内容）
ALTER TABLE plan_instance ADD COLUMN IF NOT EXISTS suggestion text;
COMMENT ON COLUMN plan_instance.suggestion IS '综合处置建议（AI 结果的 suggestion）';

-- 三级联动上报级别：0 企业运维 / 1 主管部门 / 2 应急联动（逐级 +1，上限 2）
ALTER TABLE plan_instance ADD COLUMN IF NOT EXISTS escalation_level smallint DEFAULT 0;
COMMENT ON COLUMN plan_instance.escalation_level IS '三级联动上报级别：0 企业运维 / 1 主管部门 / 2 应急联动';
