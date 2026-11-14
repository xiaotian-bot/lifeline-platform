package com.tianxin.lifeline.common.core.constant;

/**
 * Kafka Topic 常量定义。
 *
 * <p>全平台各服务收发消息统一引用本类常量，禁止硬编码 Topic 名称。
 */
public final class TopicConstants {

    private TopicConstants() {
    }

    /* ==================== 传感数据链路 ==================== */

    /** 原始传感数据 Topic（Mock/设备上报入口，保留7天） */
    public static final String PIPE_SENSOR_RAW = "pipe_sensor_raw";

    /** 清洗后标准传感数据 Topic（Flink 清洗输出） */
    public static final String PIPE_SENSOR_CLEAN = "pipe_sensor_clean";

    /* ==================== 视频识别链路 ==================== */

    /** 视频识别事件 Topic（AI 视觉分析结果） */
    public static final String PIPE_VIDEO_EVENT = "pipe_video_event";

    /* ==================== 预警链路 ==================== */

    /** 预警事件 Topic（规则引擎/AI模型产出） */
    public static final String PIPE_WARNING_EVENT = "pipe_warning_event";

    /** 预警推送记录 Topic（多渠道推送） */
    public static final String PIPE_WARNING_PUSH = "pipe_warning_push";

    /* ==================== 工单与处置链路 ==================== */

    /** 工单状态变更事件 Topic */
    public static final String PIPE_ORDER_EVENT = "pipe_order_event";

    /** 处置反馈事件 Topic */
    public static final String PIPE_DISPOSAL_EVENT = "pipe_disposal_event";

    /* ==================== 存证链路 ==================== */

    /** 区块链存证请求 Topic */
    public static final String PIPE_EVIDENCE_REQUEST = "pipe_evidence_request";

    /* ==================== 消费者组前缀 ==================== */

    /** 消费者组统一前缀 */
    public static final String GROUP_PREFIX = "lifeline-";
}
