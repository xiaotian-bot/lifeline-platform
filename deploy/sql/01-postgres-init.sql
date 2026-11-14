-- ============================================================
-- 安塞区城市安全生命线管网AI智慧平台
-- PostgreSQL 16 + PostGIS 初始化脚本
-- 包含：PostGIS 扩展 + 全部业务表 DDL（30 张表，7 大业务域）
-- 约定：表名/字段名小写下划线；每表含 id/created_at/updated_at/deleted
-- 主键 id 由应用层雪花算法生成（bigint），不使用自增序列
-- ============================================================

-- ------------------------------------------------------------
-- PostGIS 空间扩展
-- ------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================================
-- 一、系统权限域（5 表）
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          bigint PRIMARY KEY,
    username    varchar(64)  NOT NULL,
    password    varchar(128) NOT NULL,
    real_name   varchar(64),
    phone       varchar(32),
    email       varchar(128),
    status      smallint DEFAULT 1,
    dept_id     bigint,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE sys_user IS '系统用户表';
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user (username) WHERE deleted = 0;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          bigint PRIMARY KEY,
    role_name   varchar(64) NOT NULL,
    role_key    varchar(64) NOT NULL,
    sort_order  int DEFAULT 0,
    status      smallint DEFAULT 1,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE sys_role IS '系统角色表';

-- 权限表（菜单/按钮）
CREATE TABLE IF NOT EXISTS sys_permission (
    id          bigint PRIMARY KEY,
    perm_name   varchar(64) NOT NULL,
    perm_key    varchar(128) NOT NULL,
    type        smallint DEFAULT 1,
    parent_id   bigint DEFAULT 0,
    path        varchar(255),
    icon        varchar(64),
    sort_order  int DEFAULT 0,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE sys_permission IS '系统权限表（type: 1菜单 2按钮）';

-- 角色-权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            bigint PRIMARY KEY,
    role_id       bigint NOT NULL,
    permission_id bigint NOT NULL,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE sys_role_permission IS '角色权限关联表';
CREATE INDEX IF NOT EXISTS idx_srp_role_id ON sys_role_permission (role_id);

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id          bigint PRIMARY KEY,
    user_id     bigint,
    module      varchar(64),
    action      varchar(64),
    method      varchar(255),
    params      text,
    ip          varchar(64),
    result      smallint,
    error_msg   text,
    oper_time   timestamp,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE sys_oper_log IS '操作日志表';
CREATE INDEX IF NOT EXISTS idx_sol_oper_time ON sys_oper_log (oper_time);

-- ============================================================
-- 二、管网资产域（5 表）
-- ============================================================

-- 管网主线表
CREATE TABLE IF NOT EXISTS pipe_main (
    id           bigint PRIMARY KEY,
    pipe_code    varchar(64) NOT NULL,
    pipe_name    varchar(128),
    pipe_type    varchar(32),
    diameter     numeric(10,2),
    material     varchar(32),
    length       numeric(12,2),
    depth        numeric(8,2),
    install_date date,
    status       smallint DEFAULT 1,
    geom         geometry(LINESTRING, 4326),
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted      smallint DEFAULT 0
);
COMMENT ON TABLE pipe_main IS '管网主线表（pipe_type: gas/water/drainage/heating/storm）';
CREATE UNIQUE INDEX IF NOT EXISTS uk_pipe_main_code ON pipe_main (pipe_code) WHERE deleted = 0;

-- 管网扩展属性表
CREATE TABLE IF NOT EXISTS pipe_attribute (
    id          bigint PRIMARY KEY,
    pipe_id     bigint NOT NULL,
    attr_key    varchar(64),
    attr_value  varchar(255),
    attr_unit   varchar(32),
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE pipe_attribute IS '管网扩展属性表（KV结构）';
CREATE INDEX IF NOT EXISTS idx_pa_pipe_id ON pipe_attribute (pipe_id);

-- 管网设备表（传感器/监测点）
CREATE TABLE IF NOT EXISTS pipe_device (
    id           bigint PRIMARY KEY,
    device_code  varchar(64) NOT NULL,
    device_name  varchar(128),
    device_type  varchar(32),
    pipe_id      bigint,
    status       smallint DEFAULT 1,
    install_date date,
    geom         geometry(POINT, 4326),
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted      smallint DEFAULT 0
);
COMMENT ON TABLE pipe_device IS '管网设备表（device_type: pressure/temp/flow/vibration/corrosion/leakage/video）';
CREATE UNIQUE INDEX IF NOT EXISTS uk_pipe_device_code ON pipe_device (device_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_pd_pipe_id ON pipe_device (pipe_id);

-- 井室（窨井/阀门井）表
CREATE TABLE IF NOT EXISTS manhole (
    id           bigint PRIMARY KEY,
    manhole_code varchar(64) NOT NULL,
    manhole_type varchar(32),
    diameter     numeric(10,2),
    depth        numeric(8,2),
    geom         geometry(POINT, 4326),
    status       smallint DEFAULT 1,
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted      smallint DEFAULT 0
);
COMMENT ON TABLE manhole IS '井室表';
CREATE UNIQUE INDEX IF NOT EXISTS uk_manhole_code ON manhole (manhole_code) WHERE deleted = 0;

-- 管网拓扑关系表
CREATE TABLE IF NOT EXISTS pipe_topology (
    id            bigint PRIMARY KEY,
    from_pipe_id  bigint,
    to_pipe_id    bigint,
    from_node     varchar(64),
    to_node       varchar(64),
    relation_type varchar(32),
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE pipe_topology IS '管网拓扑关系表';
CREATE INDEX IF NOT EXISTS idx_pt_from_pipe ON pipe_topology (from_pipe_id);
CREATE INDEX IF NOT EXISTS idx_pt_to_pipe ON pipe_topology (to_pipe_id);

-- ============================================================
-- 三、预警域（5 表）
-- ============================================================

-- 预警事件表
CREATE TABLE IF NOT EXISTS warning_event (
    id            bigint PRIMARY KEY,
    event_code    varchar(64) NOT NULL,
    device_id     bigint,
    pipe_id       bigint,
    pipe_type     varchar(32),
    warning_level smallint,
    warning_type  varchar(64),
    description   text,
    ai_score      numeric(6,4),
    status        smallint DEFAULT 0,
    confirmed_by  bigint,
    confirmed_at  timestamp,
    resolved_at   timestamp,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE warning_event IS '预警事件表（status: 0待确认 1已确认 2处置中 3已解除 4误报）';
CREATE UNIQUE INDEX IF NOT EXISTS uk_warning_event_code ON warning_event (event_code) WHERE deleted = 0;

-- 预警规则表
CREATE TABLE IF NOT EXISTS warning_rule (
    id              bigint PRIMARY KEY,
    rule_name       varchar(128),
    rule_type       varchar(32),
    pipe_type       varchar(32),
    condition_expr  text,
    threshold_value numeric(12,4),
    warning_level   smallint,
    status          smallint DEFAULT 1,
    created_at      timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at      timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted         smallint DEFAULT 0
);
COMMENT ON TABLE warning_rule IS '预警规则表';

-- 预警等级表
CREATE TABLE IF NOT EXISTS warning_level (
    id             bigint PRIMARY KEY,
    level_code     varchar(16),
    level_name     varchar(32),
    color          varchar(16),
    response_require text,
    push_scope     varchar(255),
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted        smallint DEFAULT 0
);
COMMENT ON TABLE warning_level IS '预警等级表（蓝/黄/橙/红四级）';

-- 预警推送记录表
CREATE TABLE IF NOT EXISTS warning_push_record (
    id         bigint PRIMARY KEY,
    event_id   bigint,
    channel    varchar(32),
    receiver   varchar(128),
    content    text,
    status     smallint DEFAULT 0,
    push_time  timestamp,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted    smallint DEFAULT 0
);
COMMENT ON TABLE warning_push_record IS '预警推送记录表（channel: sms/app/wechat/voice/screen）';
CREATE INDEX IF NOT EXISTS idx_wpr_event_id ON warning_push_record (event_id);

-- 预报报告表
CREATE TABLE IF NOT EXISTS forecast_report (
    id             bigint PRIMARY KEY,
    device_id      bigint,
    pipe_id        bigint,
    forecast_type  varchar(32),
    trend_desc     varchar(255),
    risk_time      timestamp,
    confidence     numeric(6,4),
    report_content text,
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted        smallint DEFAULT 0
);
COMMENT ON TABLE forecast_report IS 'AI预报报告表';

-- ============================================================
-- 四、预案域（5 表）
-- ============================================================

-- 预案模板表
CREATE TABLE IF NOT EXISTS plan_template (
    id            bigint PRIMARY KEY,
    template_name varchar(128),
    template_type varchar(32),
    scenario_type varchar(32),
    content       text,
    status        smallint DEFAULT 1,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE plan_template IS '应急预案模板表';

-- 预案实例表
CREATE TABLE IF NOT EXISTS plan_instance (
    id                bigint PRIMARY KEY,
    instance_code     varchar(64),
    template_id       bigint,
    warning_event_id  bigint,
    status            smallint DEFAULT 0,
    generated_by      bigint,
    start_time        timestamp,
    end_time          timestamp,
    created_at        timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted           smallint DEFAULT 0
);
COMMENT ON TABLE plan_instance IS '预案执行实例表';
CREATE INDEX IF NOT EXISTS idx_pi_event_id ON plan_instance (warning_event_id);

-- 预案任务表
CREATE TABLE IF NOT EXISTS plan_task (
    id          bigint PRIMARY KEY,
    instance_id bigint,
    task_name   varchar(128),
    task_type   varchar(32),
    assignee    varchar(64),
    status      smallint DEFAULT 0,
    priority    smallint DEFAULT 2,
    start_time  timestamp,
    end_time    timestamp,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE plan_task IS '预案任务表';
CREATE INDEX IF NOT EXISTS idx_ptask_instance_id ON plan_task (instance_id);

-- 预案资源表
CREATE TABLE IF NOT EXISTS plan_resource (
    id            bigint PRIMARY KEY,
    instance_id   bigint,
    resource_type varchar(32),
    resource_name varchar(128),
    quantity      numeric(12,2),
    location      varchar(255),
    status        smallint DEFAULT 0,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE plan_resource IS '预案资源表';
CREATE INDEX IF NOT EXISTS idx_pres_instance_id ON plan_resource (instance_id);

-- 预案执行日志表
CREATE TABLE IF NOT EXISTS plan_log (
    id          bigint PRIMARY KEY,
    instance_id bigint,
    action      varchar(64),
    operator    varchar(64),
    content     text,
    log_time    timestamp,
    created_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at  timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted     smallint DEFAULT 0
);
COMMENT ON TABLE plan_log IS '预案执行日志表';
CREATE INDEX IF NOT EXISTS idx_plog_instance_id ON plan_log (instance_id);

-- ============================================================
-- 五、施工域（4 表）
-- ============================================================

-- 施工项目表
CREATE TABLE IF NOT EXISTS construction_project (
    id           bigint PRIMARY KEY,
    project_name varchar(128),
    location     varchar(255),
    contractor   varchar(128),
    supervisor   varchar(64),
    start_date   date,
    end_date     date,
    status       smallint DEFAULT 1,
    geom         geometry(POINT, 4326),
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted      smallint DEFAULT 0
);
COMMENT ON TABLE construction_project IS '施工项目表';

-- 施工工序表
CREATE TABLE IF NOT EXISTS construction_process (
    id            bigint PRIMARY KEY,
    project_id    bigint,
    process_name  varchar(128),
    process_type  varchar(32),
    status        smallint DEFAULT 0,
    inspector     varchar(64),
    inspect_time  timestamp,
    quality_score numeric(6,2),
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE construction_process IS '施工工序表';
CREATE INDEX IF NOT EXISTS idx_cp_project_id ON construction_process (project_id);

-- 施工违规记录表
CREATE TABLE IF NOT EXISTS construction_violation (
    id             bigint PRIMARY KEY,
    project_id     bigint,
    violation_type varchar(64),
    description    text,
    evidence_url   varchar(512),
    status         smallint DEFAULT 0,
    blockchain_hash varchar(128),
    created_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at     timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted        smallint DEFAULT 0
);
COMMENT ON TABLE construction_violation IS '施工违规记录表（区块链存证）';
CREATE INDEX IF NOT EXISTS idx_cv_project_id ON construction_violation (project_id);

-- 施工影像表
CREATE TABLE IF NOT EXISTS construction_media (
    id           bigint PRIMARY KEY,
    project_id   bigint,
    media_type   varchar(16),
    file_url     varchar(512),
    capture_time timestamp,
    camera_id    varchar(64),
    created_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted      smallint DEFAULT 0
);
COMMENT ON TABLE construction_media IS '施工影像表（media_type: image/video）';
CREATE INDEX IF NOT EXISTS idx_cm_project_id ON construction_media (project_id);

-- ============================================================
-- 六、运维域（4 表）
-- ============================================================

-- 运维工单表
CREATE TABLE IF NOT EXISTS operation_order (
    id               bigint PRIMARY KEY,
    order_code       varchar(64) NOT NULL,
    order_type       varchar(32),
    warning_event_id bigint,
    device_id        bigint,
    pipe_id          bigint,
    assignee         varchar(64),
    status           smallint DEFAULT 0,
    priority         smallint DEFAULT 2,
    content          text,
    result           text,
    start_time       timestamp,
    end_time         timestamp,
    created_at       timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at       timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted          smallint DEFAULT 0
);
COMMENT ON TABLE operation_order IS '运维工单表（status: 0待派单 1待接单 2处置中 3已完成 4已关闭）';
CREATE UNIQUE INDEX IF NOT EXISTS uk_operation_order_code ON operation_order (order_code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_oo_event_id ON operation_order (warning_event_id);

-- 巡检计划表
CREATE TABLE IF NOT EXISTS inspection_plan (
    id         bigint PRIMARY KEY,
    plan_name  varchar(128),
    plan_type  varchar(32),
    area_id    varchar(64),
    frequency  varchar(32),
    status     smallint DEFAULT 1,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted    smallint DEFAULT 0
);
COMMENT ON TABLE inspection_plan IS '巡检计划表';

-- 运维人员表
CREATE TABLE IF NOT EXISTS operation_staff (
    id         bigint PRIMARY KEY,
    staff_code varchar(64),
    staff_name varchar(64),
    skill_type varchar(32),
    phone      varchar(32),
    status     smallint DEFAULT 1,
    dept_id    bigint,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted    smallint DEFAULT 0
);
COMMENT ON TABLE operation_staff IS '运维人员表';

-- 运维物资表
CREATE TABLE IF NOT EXISTS operation_material (
    id            bigint PRIMARY KEY,
    material_name varchar(128),
    material_type varchar(32),
    stock         numeric(12,2) DEFAULT 0,
    unit          varchar(16),
    warehouse     varchar(64),
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE operation_material IS '运维物资表';

-- ============================================================
-- 七、区块链域（2 表）
-- ============================================================

-- 区块链DID表（管网数字身份）
CREATE TABLE IF NOT EXISTS blockchain_did (
    id         bigint PRIMARY KEY,
    did        varchar(128),
    pipe_id    bigint,
    pipe_code  varchar(64),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted    smallint DEFAULT 0
);
COMMENT ON TABLE blockchain_did IS '区块链DID表（管网数字身份）';
CREATE INDEX IF NOT EXISTS idx_bd_pipe_id ON blockchain_did (pipe_id);

-- 区块链存证表
CREATE TABLE IF NOT EXISTS blockchain_evidence (
    id            bigint PRIMARY KEY,
    evidence_hash varchar(128),
    evidence_type varchar(32),
    related_id    bigint,
    related_type  varchar(32),
    content       text,
    chain_status  smallint DEFAULT 0,
    chain_time    timestamp,
    created_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at    timestamp DEFAULT CURRENT_TIMESTAMP,
    deleted       smallint DEFAULT 0
);
COMMENT ON TABLE blockchain_evidence IS '区块链存证表';
CREATE INDEX IF NOT EXISTS idx_be_hash ON blockchain_evidence (evidence_hash);

-- ============================================================
-- 空间索引与复合索引
-- ============================================================

-- pipe_main GiST 空间索引（空间查询加速）
CREATE INDEX IF NOT EXISTS idx_pipe_main_geom_gist ON pipe_main USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_pipe_device_geom_gist ON pipe_device USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_manhole_geom_gist ON manhole USING GIST (geom);

-- warning_event 复合索引（等级+状态+时间，预警列表高频查询）
CREATE INDEX IF NOT EXISTS idx_we_level_status_time ON warning_event (warning_level, status, created_at);
