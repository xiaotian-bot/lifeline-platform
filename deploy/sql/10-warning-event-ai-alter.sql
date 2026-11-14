-- ============================================================
-- 10-warning-event-ai-alter.sql
-- 预警事件表（warning_event）AI 研判结果增量补列脚本
--
-- 背景：warning-service 打通 AI 研判实接链路——预警事件落库后异步调用
--       ai-gateway-service 智能研判代理（/agent/judge），需持久化
--       研判结论与研判完成标记。ai_score 列已存在于 01 号脚本
--       （numeric(6,4)），本脚本仅补研判结论文本与研判时间，
--       以幂等方式（IF NOT EXISTS）执行，可重复执行不报错。
-- 执行说明：存量已初始化环境必须手工执行本脚本（幂等可重放），
--       docker-entrypoint-initdb.d 仅首启生效，不会自动补跑。
-- ============================================================

-- AI 研判结论摘要（根因分析/等级复核意见/影响范围/趋势预测/处置优先级）
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS ai_result varchar(2000);
COMMENT ON COLUMN warning_event.ai_result IS 'AI 研判结论摘要（根因/等级复核/影响范围/趋势/处置优先级），未研判为 NULL';

-- AI 研判完成时间（兼作「已研判」幂等标记，非 NULL 即已完成研判）
ALTER TABLE warning_event ADD COLUMN IF NOT EXISTS ai_judged_at timestamp;
COMMENT ON COLUMN warning_event.ai_judged_at IS 'AI 研判完成时间（非 NULL 表示已完成 AI 研判，用于去重）';
