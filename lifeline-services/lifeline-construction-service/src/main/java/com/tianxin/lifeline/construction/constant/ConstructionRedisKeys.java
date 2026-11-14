package com.tianxin.lifeline.construction.constant;

/**
 * 施工监护服务 Redis 键常量与构造工具。
 *
 * <p>人工违规上报幂等键：按「项目 + 违规类型 + 描述摘要」SETNX 去重，
 * 防止前端重复提交产生重复违规记录与重复存证事件。
 */
public final class ConstructionRedisKeys {

    private ConstructionRedisKeys() {
    }

    /** 人工违规上报幂等键前缀 */
    public static final String VIOLATION_REPORT_IDEMPOTENT_PREFIX = "lifeline:construction:idem:violation-report:";

    /** AI 视觉质检处理中标记前缀（防同工序并发重复发起视觉分析） */
    public static final String VISION_INSPECT_LOCK_PREFIX = "lifeline:construction:lock:vision-inspect:";

    /**
     * 构建人工违规上报幂等键。
     *
     * @param fingerprint 上报指纹（项目ID + 违规类型 + 描述摘要的散列值）
     * @return Redis 键
     */
    public static String violationReportIdempotent(String fingerprint) {
        return VIOLATION_REPORT_IDEMPOTENT_PREFIX + fingerprint;
    }

    /**
     * 构建视觉质检并发锁键。
     *
     * @param processId 工序ID
     * @return Redis 键
     */
    public static String visionInspectLock(Long processId) {
        return VISION_INSPECT_LOCK_PREFIX + processId;
    }
}
