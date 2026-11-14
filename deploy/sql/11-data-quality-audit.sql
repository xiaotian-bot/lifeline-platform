-- ============================================================
-- 11-data-quality-audit.sql
-- 数据质量稽核结果表（P10 Spark DataQualityJob 产出）
--
-- 背景：DataQualityJob 对 TDengine sensor_data 原始监测数据做
--       四维质量稽核（完整性 / 有效性 / 一致性 / 时效性），
--       结果按 设备×日×维度 粒度写入本表，供运维看板与告警使用。
-- 说明：全部语句幂等（IF NOT EXISTS），可重复执行不报错。
-- 执行：存量已初始化环境须手工执行（docker exec psql 重放），
--       docker-entrypoint-initdb.d 仅首启生效。
-- ============================================================

-- 数据质量稽核结果表
CREATE TABLE IF NOT EXISTS data_quality_audit (
    id              bigint PRIMARY KEY,
    audit_date      date         NOT NULL,
    device_id       varchar(64)  NOT NULL,
    pipe_type       varchar(32),
    dimension       varchar(32)  NOT NULL,
    expected_count  bigint       DEFAULT 0,
    actual_count    bigint       DEFAULT 0,
    pass_count      bigint       DEFAULT 0,
    fail_count      bigint       DEFAULT 0,
    pass_rate       numeric(6,4) DEFAULT 0,
    fail_detail     text,
    created_at      timestamp    DEFAULT CURRENT_TIMESTAMP,
    updated_at      timestamp    DEFAULT CURRENT_TIMESTAMP,
    deleted         smallint     DEFAULT 0
);
COMMENT ON TABLE data_quality_audit IS '数据质量稽核结果表（dimension: COMPLETENESS/VALIDITY/CONSISTENCY/TIMELINESS）';

-- 唯一约束：同日同设备同维度仅一条稽核结果（幂等 upsert 锚点）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'data_quality_audit'
          AND indexname = 'uk_dqa_date_device_dim'
    ) THEN
        CREATE UNIQUE INDEX uk_dqa_date_device_dim
            ON data_quality_audit (audit_date, device_id, dimension)
            WHERE deleted = 0;
    END IF;
END $$;

-- 按日期查询索引（运维看板按日筛选）
CREATE INDEX IF NOT EXISTS idx_dqa_audit_date ON data_quality_audit (audit_date);
-- 按设备查询索引（设备维度质量趋势）
CREATE INDEX IF NOT EXISTS idx_dqa_device_id ON data_quality_audit (device_id);
