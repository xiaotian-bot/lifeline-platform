package com.tianxin.lifeline.warning.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预警推送 Kafka 负载 DTO。
 *
 * <p>预警事件落库后按等级匹配推送对象，逐接收人生成本负载，
 * 经 EventEnvelope 信封投递至 {@code pipe_warning_push}，
 * 由 WarningPushListener 消费落库 warning_push_record 留痕。
 */
@Data
public class WarningPushPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联预警事件ID */
    private Long eventId;

    /** 预警编号 */
    private String warningNo;

    /** 预警等级编码：BLUE / YELLOW / ORANGE / RED */
    private String warningLevel;

    /** 推送渠道：sms/app/wechat/voice/screen */
    private String channel;

    /** 接收对象（角色或人员名称） */
    private String receiver;

    /** 推送内容 */
    private String content;

    /** 计划推送时间 */
    private LocalDateTime pushTime;
}
