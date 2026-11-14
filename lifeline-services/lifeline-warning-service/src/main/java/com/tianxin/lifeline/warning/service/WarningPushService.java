package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.warning.dto.WarningPushPayload;
import com.tianxin.lifeline.warning.entity.WarningEvent;

/**
 * 预警推送服务。
 *
 * <p>预警事件落库后，按等级匹配 warning_level 表的 push_scope（推送对象）
 * 与 response_require（响应要求），逐接收人生成推送负载并经 Kafka
 * pipe_warning_push 异步化；消费端落库 warning_push_record 留痕。
 */
public interface WarningPushService {

    /**
     * 按预警等级生成推送并经 Kafka 异步投递。
     *
     * @param event 已落库的预警事件
     */
    void publishPushes(WarningEvent event);

    /**
     * 保存推送记录（消费端落库留痕）。
     *
     * @param payload 推送负载
     */
    void savePushRecord(WarningPushPayload payload);
}
