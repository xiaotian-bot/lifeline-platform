-- ============================================================
-- 安塞区城市安全生命线管网AI智慧平台 - P9 域种子数据
-- 覆盖：预案域（plan_template）/ 运维域（operation_staff、
--       operation_material、inspection_plan）/ 施工域
--       （construction_project、construction_process）/
--       预警域（warning_rule 规则种子）
-- 依赖：01-postgres-init.sql（表结构）、03-seed-data.sql（基础数据）
-- ID 分段约定：9001 预案模板 / 9101 运维人员 / 9201 运维物资 /
--   9301 巡检计划 / 9401 施工项目 / 9411 施工工序 / 9501 预警规则
-- 注意：全部 INSERT 采用 ON CONFLICT (id) DO NOTHING 幂等风格，可重复执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 应急预案模板（八大类场景）
--    template_type：EMERGENCY 应急处置预案
--    scenario_type：GAS_LEAK 燃气泄漏 / WATER_BURST 供水爆管 /
--      HEATING_LEAK 供热泄漏 / DRAINAGE_OVERFLOW 排水溢流 /
--      ROAD_COLLAPSE 道路塌陷 / THIRD_PARTY_DAMAGE 第三方施工破坏 /
--      CORROSION_FAILURE 腐蚀失效 / COMPREHENSIVE 综合应急
-- ------------------------------------------------------------
INSERT INTO plan_template (id, template_name, template_type, scenario_type, content, status) VALUES
(9001, '燃气泄漏应急处置预案',   'EMERGENCY', 'GAS_LEAK',          '处置要点：1.立即切断泄漏点上下游阀门，划定50米警戒区并禁绝明火；2.可燃气体浓度检测持续低于爆炸下限20%方可作业；3.采用PE管热熔修补或换管，修复后气密性试验合格方可恢复供气；4.同步通知燃气企业与消防救援联动。', 1),
(9002, '供水爆管应急处置预案',   'EMERGENCY', 'WATER_BURST',       '处置要点：1.关断爆管段控制阀门，启动备用供水路径保障民生用水；2.开挖探明破损点，评估管材破损范围；3.球墨铸铁管采用抢修节/套筒修复，修复后冲洗试压达标恢复供水；4.做好泄水疏导防止次生浸泡灾害。', 1),
(9003, '供热泄漏应急处置预案',   'EMERGENCY', 'HEATING_LEAK',      '处置要点：1.非采暖季带温带压堵漏或采暖季停热隔离泄漏段；2.做好高温蒸汽防烫伤防护，作业前测温确认；3.预制直埋管更换需补口保温，恢复后水力平衡调试；4.同步通知受影响换热站与用户。', 1),
(9004, '排水溢流应急处置预案',   'EMERGENCY', 'DRAINAGE_OVERFLOW', '处置要点：1.溢流点围挡导流，防止污水漫溢入户；2.疏通/吸污车清理淤堵段，排查混接错接；3.结构性缺陷安排内衬修复或换管；4.雨前预警联动泵站提前预抽腾空。', 1),
(9005, '道路塌陷应急处置预案',   'EMERGENCY', 'ROAD_COLLAPSE',     '处置要点：1.立即封闭塌陷区域交通，设置围挡与警示；2.雷达探测确认地下空洞范围；3.回填注浆加固后再行管线修复；4.修复后承载力检测合格方可恢复通行，全程监测周边沉降。', 1),
(9006, '第三方施工破坏处置预案', 'EMERGENCY', 'THIRD_PARTY_DAMAGE','处置要点：1.责令施工方立即停工并撤离机械；2.评估管线受损情况，燃气类先断气测爆再作业；3.留存影像证据并启动责任认定与索赔；4.修复后经巡检确认方可允许复工。', 1),
(9007, '腐蚀失效应急处置预案',   'EMERGENCY', 'CORROSION_FAILURE', '处置要点：1.对腐蚀点位开挖验证，测壁厚评估剩余强度；2.局部腐蚀采用补强套管，大面积腐蚀整体换管；3.修复后补做防腐层并纳入阴极保护巡检；4.同类管段加密检测频次。', 1),
(9008, '综合应急联动预案',       'EMERGENCY', 'COMPREHENSIVE',     '处置要点：1.多灾种叠加时启动应急指挥中心统一调度；2.按红橙黄蓝等级执行分级响应与上报机制；3.物资/人员/设备统一调配，优先保障燃气与供水生命线；4.事后复盘归档并更新专项预案。', 1)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 运维人员（5 名，技能覆盖 燃气/供水/供热/排水/综合）
-- ------------------------------------------------------------
INSERT INTO operation_staff (id, staff_code, staff_name, skill_type, phone, status, dept_id) VALUES
(9101, 'STAFF-001', '王建国', 'gas',       '13900000101', 1, 9003),
(9102, 'STAFF-002', '李红梅', 'water',     '13900000102', 1, 9003),
(9103, 'STAFF-003', '张铁柱', 'heating',   '13900000103', 1, 9003),
(9104, 'STAFF-004', '刘雨欣', 'drainage',  '13900000104', 1, 9003),
(9105, 'STAFF-005', '赵国栋', 'composite', '13900000105', 1, 9003)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 运维物资（5 种常用抢修物资）
-- ------------------------------------------------------------
INSERT INTO operation_material (id, material_name, material_type, stock, unit, warehouse) VALUES
(9201, '应急抢修阀门（DN300）',     'valve',     20.00,   '个',  '城区一号应急仓库'),
(9202, '球墨铸铁抢修管材（DN400）', 'pipe',      150.00,  '米',  '城区一号应急仓库'),
(9203, '管道抢修套筒（DN300-500）', 'clamp',     30.00,   '套',  '城区一号应急仓库'),
(9204, '环氧防腐修补材料',          'anticorr',  500.00,  '公斤','二号物资库'),
(9205, '聚氨酯保温棉（供热补口用）','insulation',800.00,  '卷',  '二号物资库')
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 巡检计划（3 条：定期巡查 / 燃气专项 / 雨季排水专项）
--    frequency：DAILY 每日 / WEEKLY 每周 / MONTHLY 每月
-- ------------------------------------------------------------
INSERT INTO inspection_plan (id, plan_name, plan_type, area_id, frequency, status) VALUES
(9301, '城区管网日常定期巡查',   'ROUTINE',  '610603', 'DAILY',   1),
(9302, '燃气管网专项巡检',       'GAS',      '610603', 'WEEKLY',  1),
(9303, '雨季排水管网专项巡检',   'DRAINAGE', '610603', 'MONTHLY', 1)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 施工项目示例（1 个）+ 施工工序（2 条）
-- ------------------------------------------------------------
INSERT INTO construction_project (id, project_name, location, contractor, supervisor, start_date, end_date, status, geom) VALUES
(9401, '迎宾大道供水管网改造工程', '安塞区迎宾大道与延河路交叉口东侧', '延安市政建设集团有限公司', '陈志刚', '2026-07-01', '2026-10-31', 1,
 ST_GeomFromText('POINT(109.3275 36.8605)', 4326))
ON CONFLICT (id) DO NOTHING;

INSERT INTO construction_process (id, project_id, process_name, process_type, status, inspector, inspect_time, quality_score) VALUES
(9411, 9401, '沟槽开挖与支护',   'EXCAVATION', 2, '陈志刚', '2026-07-20 10:30:00', 92.50),
(9412, 9401, '管道安装与接口连接', 'PIPE_LAYING', 1, '陈志刚', NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 预警规则种子（覆盖 Mock pressure_drop 场景）
--    背景：Mock 压力基线约 0.4MPa，压降场景最大跌落 85%（≈0.06MPa）
--    condition_expr 文本约定："指标 < threshold" 表示低于阈值触发
--    warning_level：1 红 / 2 橙 / 3 黄 / 4 蓝
-- ------------------------------------------------------------
INSERT INTO warning_rule (id, rule_name, rule_type, pipe_type, condition_expr, threshold_value, warning_level, status) VALUES
(9501, '燃气管网压力骤降红色预警', 'PRESSURE_LOW', 'gas',     'pressure < threshold', 0.1500, 1, 1),
(9502, '燃气管网压力偏低橙色预警', 'PRESSURE_LOW', 'gas',     'pressure < threshold', 0.2500, 2, 1),
(9503, '供水管网压力偏低橙色预警', 'PRESSURE_LOW', 'water',   'pressure < threshold', 0.2000, 2, 1),
(9504, '供热管网压力骤降红色预警', 'PRESSURE_LOW', 'heating', 'pressure < threshold', 0.1500, 1, 1),
(9505, '排水管网流量超限黄色预警', 'FLOW_HIGH',    'drainage','flow > threshold',     0.8500, 3, 1)
ON CONFLICT (id) DO NOTHING;
