-- ============================================================
-- 安塞区城市安全生命线管网AI智慧平台 - 种子数据
-- 依赖：01-postgres-init.sql（表结构先行创建）
-- 设备ID命名规范：DEV-{管网类型}-{三位序号}，与 Mock 数据模拟系统对齐
--   管网类型：GAS燃气 / WAT给水 / DRN排水 / HTG供热 / STR雨水
-- 注意：用户密码字段为 BCrypt 加密结果（明文 admin123），
--       首次上线前请由安全模块重新生成。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 管理员用户（5 个）
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, phone, email, status, dept_id) VALUES
(1001, 'admin',      '$2a$10$3AKmfU.CfkN4U4.aXZaCx.mnHzpk/guw5VMleFsvoTHNBuoI2Yck2', '系统管理员',   '13800000001', 'admin@lifeline.gov.cn',      1, 9001),
(1002, 'government', '$2a$10$3AKmfU.CfkN4U4.aXZaCx.mnHzpk/guw5VMleFsvoTHNBuoI2Yck2', '政府监管员',   '13800000002', 'government@lifeline.gov.cn', 1, 9002),
(1003, 'operator',   '$2a$10$3AKmfU.CfkN4U4.aXZaCx.mnHzpk/guw5VMleFsvoTHNBuoI2Yck2', '运维操作员',   '13800000003', 'operator@lifeline.gov.cn',   1, 9003),
(1004, 'enterprise', '$2a$10$3AKmfU.CfkN4U4.aXZaCx.mnHzpk/guw5VMleFsvoTHNBuoI2Yck2', '企业用户',     '13800000004', 'enterprise@lifeline.gov.cn', 1, 9004),
(1005, 'guest',      '$2a$10$3AKmfU.CfkN4U4.aXZaCx.mnHzpk/guw5VMleFsvoTHNBuoI2Yck2', '访客',         '13800000005', 'guest@lifeline.gov.cn',      1, 9005)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 2. 角色（5 个）
-- ------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_key, sort_order, status) VALUES
(2001, '超级管理员', 'ROLE_ADMIN',      1, 1),
(2002, '政府监管',   'ROLE_GOVERNMENT', 2, 1),
(2003, '运维操作',   'ROLE_OPERATOR',   3, 1),
(2004, '企业用户',   'ROLE_ENTERPRISE', 4, 1),
(2005, '访客',       'ROLE_GUEST',      5, 1)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 3. 预警等级（蓝/黄/橙/红 四级）
-- ------------------------------------------------------------
INSERT INTO warning_level (id, level_code, level_name, color, response_require, push_scope) VALUES
(3001, 'LEVEL_4', '蓝色预警', '#1E88E5', '值班人员关注监测数据，加强巡查频次', '值班人员,运维班组'),
(3002, 'LEVEL_3', '黄色预警', '#FDD835', '运维班组30分钟内响应，现场核查确认', '运维班组,运维主管'),
(3003, 'LEVEL_2', '橙色预警', '#FB8C00', '启动应急预案，15分钟内到场处置，上报主管部门', '运维主管,主管部门,应急指挥中心'),
(3004, 'LEVEL_1', '红色预警', '#E53935', '立即启动最高级应急响应，多部门联动处置，实时上报区政府', '应急指挥中心,区政府,全部相关方')
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 4. 管网主线（5 条，坐标系 EPSG:4326，安塞区城区范围）
-- ------------------------------------------------------------
INSERT INTO pipe_main (id, pipe_code, pipe_name, pipe_type, diameter, material, length, depth, install_date, status, geom) VALUES
(4001, 'PM-GAS-001', '城区燃气中压干线-真武洞段', 'gas',     300.00, 'PE',   2350.50, 1.20, '2018-06-15', 1,
 ST_GeomFromText('LINESTRING(109.3260 36.8620, 109.3305 36.8642, 109.3352 36.8658)', 4326)),
(4002, 'PM-WAT-001', '城区给水主干管-迎宾大道段', 'water',   400.00, '球墨铸铁', 3120.80, 1.50, '2016-09-20', 1,
 ST_GeomFromText('LINESTRING(109.3180 36.8575, 109.3240 36.8598, 109.3310 36.8612)', 4326)),
(4003, 'PM-DRN-001', '城区排水主管-延河路段',    'drainage', 800.00, '钢筋混凝土', 2780.30, 2.80, '2014-05-10', 1,
 ST_GeomFromText('LINESTRING(109.3210 36.8530, 109.3268 36.8555, 109.3330 36.8570)', 4326)),
(4004, 'PM-HTG-001', '城区供热一次网-城北段',    'heating',  500.00, '预制直埋钢管', 3450.60, 1.80, '2019-08-25', 1,
 ST_GeomFromText('LINESTRING(109.3225 36.8680, 109.3280 36.8702, 109.3340 36.8715)', 4326)),
(4005, 'PM-STR-001', '城区雨水管-滨河段',        'storm',    600.00, 'HDPE', 1980.40, 2.20, '2020-04-18', 1,
 ST_GeomFromText('LINESTRING(109.3295 36.8510, 109.3345 36.8532, 109.3398 36.8548)', 4326))
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 5. 监测设备（10 个，与 Mock 数据模拟系统 device_id 对齐）
--    device_type：pressure压力 / flow流量 / temperature温度 / vibration振动 / leakage泄漏
-- ------------------------------------------------------------
INSERT INTO pipe_device (id, device_code, device_name, device_type, pipe_id, status, install_date, geom) VALUES
(5001, 'DEV-GAS-001', '燃气压力传感器-真武洞A点', 'pressure',    4001, 1, '2024-03-10', ST_GeomFromText('POINT(109.3260 36.8620)', 4326)),
(5002, 'DEV-GAS-002', '燃气泄漏探测器-真武洞B点', 'leakage',     4001, 1, '2024-03-10', ST_GeomFromText('POINT(109.3352 36.8658)', 4326)),
(5003, 'DEV-WAT-001', '给水压力传感器-迎宾A点',   'pressure',    4002, 1, '2024-03-12', ST_GeomFromText('POINT(109.3180 36.8575)', 4326)),
(5004, 'DEV-WAT-002', '给水流量计-迎宾B点',       'flow',        4002, 1, '2024-03-12', ST_GeomFromText('POINT(109.3310 36.8612)', 4326)),
(5005, 'DEV-DRN-001', '排水液位流量传感器-延河A点', 'flow',      4003, 1, '2024-03-15', ST_GeomFromText('POINT(109.3210 36.8530)', 4326)),
(5006, 'DEV-DRN-002', '排水有害气体探测器-延河B点', 'leakage',   4003, 1, '2024-03-15', ST_GeomFromText('POINT(109.3330 36.8570)', 4326)),
(5007, 'DEV-HTG-001', '供热温度传感器-城北A点',   'temperature', 4004, 1, '2024-03-18', ST_GeomFromText('POINT(109.3225 36.8680)', 4326)),
(5008, 'DEV-HTG-002', '供热压力传感器-城北B点',   'pressure',    4004, 1, '2024-03-18', ST_GeomFromText('POINT(109.3340 36.8715)', 4326)),
(5009, 'DEV-STR-001', '雨水液位传感器-滨河A点',   'flow',        4005, 1, '2024-03-20', ST_GeomFromText('POINT(109.3295 36.8510)', 4326)),
(5010, 'DEV-STR-002', '雨水管网振动监测-滨河B点', 'vibration',   4005, 1, '2024-03-20', ST_GeomFromText('POINT(109.3398 36.8548)', 4326))
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 6. 基础权限数据（一级菜单 + 常用按钮权限）
-- ------------------------------------------------------------
INSERT INTO sys_permission (id, perm_name, perm_key, type, parent_id, path, icon, sort_order) VALUES
(6001, '监测大屏',   'monitor:screen',    1, 0, '/monitor/screen',    'Monitor',    1),
(6002, '实时监测',   'monitor:realtime',  1, 0, '/monitor/realtime',  'Odometer',   2),
(6003, '预警中心',   'warning:center',    1, 0, '/warning/center',    'Bell',       3),
(6004, '预报分析',   'warning:forecast',  1, 0, '/warning/forecast',  'TrendCharts',4),
(6005, '预案管理',   'plan:manage',       1, 0, '/plan/manage',       'Folder',     5),
(6006, '施工监管',   'construction:supervise', 1, 0, '/construction/supervise', 'OfficeBuilding', 6),
(6007, '运维工单',   'operation:order',   1, 0, '/operation/order',   'Tickets',    7),
(6008, '资产台账',   'asset:ledger',      1, 0, '/asset/ledger',      'Grid',       8),
(6009, '系统管理',   'system:manage',     1, 0, '/system/manage',     'Setting',    9),
(6101, '预警确认',   'warning:event:confirm', 2, 6003, NULL, NULL, 1),
(6102, '预警解除',   'warning:event:resolve', 2, 6003, NULL, NULL, 2),
(6103, '工单派发',   'operation:order:assign', 2, 6007, NULL, NULL, 1),
(6104, '工单处置',   'operation:order:handle', 2, 6007, NULL, NULL, 2),
(6105, '预案启动',   'plan:instance:start',    2, 6005, NULL, NULL, 1),
(6106, '用户管理',   'system:user:manage',     2, 6009, NULL, NULL, 1)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- 7. 角色-权限关联（管理员全量，其余角色按需）
-- ------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(7001, 2001, 6001), (7002, 2001, 6002), (7003, 2001, 6003), (7004, 2001, 6004),
(7005, 2001, 6005), (7006, 2001, 6006), (7007, 2001, 6007), (7008, 2001, 6008),
(7009, 2001, 6009), (7010, 2001, 6101), (7011, 2001, 6102), (7012, 2001, 6103),
(7013, 2001, 6104), (7014, 2001, 6105), (7015, 2001, 6106),
(7016, 2002, 6001), (7017, 2002, 6003), (7018, 2002, 6004), (7019, 2002, 6006),
(7020, 2002, 6101), (7021, 2002, 6102),
(7022, 2003, 6001), (7023, 2003, 6002), (7024, 2003, 6003), (7025, 2003, 6007),
(7026, 2003, 6103), (7027, 2003, 6104), (7028, 2003, 6105),
(7029, 2004, 6001), (7030, 2004, 6006),
(7031, 2005, 6001)
ON CONFLICT (id) DO NOTHING;
