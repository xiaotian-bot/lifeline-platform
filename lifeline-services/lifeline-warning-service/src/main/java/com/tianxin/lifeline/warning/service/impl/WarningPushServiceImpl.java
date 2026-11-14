package com.tianxin.lifeline.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.core.constant.WarningLevel;
import com.tianxin.lifeline.common.mq.template.KafkaEventTemplate;
import com.tianxin.lifeline.warning.constant.WarningSource;
import com.tianxin.lifeline.warning.dto.WarningPushPayload;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import com.tianxin.lifeline.warning.entity.WarningLevelEntity;
import com.tianxin.lifeline.warning.entity.WarningPushRecord;
import com.tianxin.lifeline.warning.mapper.WarningLevelMapper;
import com.tianxin.lifeline.warning.mapper.WarningPushRecordMapper;
import com.tianxin.lifeline.warning.service.WarningPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 预警推送服务实现。
 *
 * <p>推送链路：预警事件落库 → 按等级查询 warning_level 配置
 * （push_scope 推送对象 / response_require 响应要求）→ 逐接收人构建
 * 推送负载 → 经 Kafka {@code pipe_warning_push} 异步化（本项目无 RabbitMQ，
 * 以 Kafka 替代异步队列）→ WarningPushListener 消费落库留痕。
 *
 * <p>推送渠道按等级差异化：红色 voice 电话 / 橙色 sms 短信 /
 * 黄色 app 应用消息 / 蓝色 screen 大屏提示。
 */
@Slf4j
@Service
public class WarningPushServiceImpl implements WarningPushService {

    /** 推送 Kafka 事件类型 */
    private static final String EVENT_TYPE_WARNING_PUSH = "WARNING_PUSH";

    /** 事件来源服务名 */
    private static final String EVENT_SOURCE = "warning-service";

    /** 等级配置缺失时的兜底推送对象 */
    private static final String DEFAULT_RECEIVER = "值班人员";

    /** 预警等级配置 Mapper */
    private final WarningLevelMapper warningLevelMapper;

    /** 推送记录 Mapper */
    private final WarningPushRecordMapper warningPushRecordMapper;

    /** Kafka 事件发送模板 */
    private final KafkaEventTemplate kafkaEventTemplate;

    /**
     * 构造推送服务。
     *
     * @param warningLevelMapper     预警等级配置 Mapper
     * @param warningPushRecordMapper 推送记录 Mapper
     * @param kafkaEventTemplate     Kafka 事件发送模板
     */
    public WarningPushServiceImpl(WarningLevelMapper warningLevelMapper,
                                  WarningPushRecordMapper warningPushRecordMapper,
                                  KafkaEventTemplate kafkaEventTemplate) {
        this.warningLevelMapper = warningLevelMapper;
        this.warningPushRecordMapper = warningPushRecordMapper;
        this.kafkaEventTemplate = kafkaEventTemplate;
    }

    /**
     * 按预警等级生成推送并经 Kafka 异步投递。
     *
     * @param event 已落库的预警事件
     */
    @Override
    public void publishPushes(WarningEvent event) {
        WarningLevel level = event.getWarningLevel() == null
                ? null : WarningLevel.fromLevel(event.getWarningLevel());
        if (level == null) {
            log.warn("预警事件等级非法, 跳过推送: warningNo={}", event.getEventCode());
            return;
        }
        // 按等级编码（LEVEL_1~LEVEL_4）查询等级配置
        WarningLevelEntity levelConfig = warningLevelMapper.selectOne(
                new LambdaQueryWrapper<WarningLevelEntity>()
                        .eq(WarningLevelEntity::getLevelCode, "LEVEL_" + level.getLevel())
                        .last("LIMIT 1"));

        List<String> receivers = parseReceivers(levelConfig);
        String channel = channelOf(level);
        String content = buildContent(event, level, levelConfig);

        for (String receiver : receivers) {
            WarningPushPayload payload = new WarningPushPayload();
            payload.setEventId(event.getId());
            payload.setWarningNo(event.getEventCode());
            payload.setWarningLevel(level.getCode());
            payload.setChannel(channel);
            payload.setReceiver(receiver);
            payload.setContent(content);
            payload.setPushTime(LocalDateTime.now());
            // 分区键取预警事件ID，保证同一预警的推送有序
            kafkaEventTemplate.sendEvent(TopicConstants.PIPE_WARNING_PUSH,
                    String.valueOf(event.getId()), EVENT_TYPE_WARNING_PUSH, EVENT_SOURCE, payload);
        }
        log.info("预警推送已投递: warningNo={}, level={}, channel={}, receivers={}",
                event.getEventCode(), level.getCode(), channel, receivers);
    }

    /**
     * 保存推送记录（消费端落库留痕）。
     *
     * @param payload 推送负载
     */
    @Override
    public void savePushRecord(WarningPushPayload payload) {
        WarningPushRecord record = new WarningPushRecord();
        record.setEventId(payload.getEventId());
        record.setChannel(payload.getChannel());
        record.setReceiver(payload.getReceiver());
        record.setContent(payload.getContent());
        record.setStatus(WarningPushRecord.STATUS_SENT);
        record.setPushTime(payload.getPushTime() == null ? LocalDateTime.now() : payload.getPushTime());
        warningPushRecordMapper.insert(record);
        log.debug("预警推送记录落库: eventId={}, receiver={}, channel={}",
                payload.getEventId(), payload.getReceiver(), payload.getChannel());
    }

    /**
     * 解析等级配置的推送对象（逗号分隔），配置缺失时兜底值班人员。
     *
     * @param levelConfig 等级配置
     * @return 接收人列表
     */
    private List<String> parseReceivers(WarningLevelEntity levelConfig) {
        List<String> receivers = new ArrayList<>();
        if (levelConfig != null && levelConfig.getPushScope() != null && !levelConfig.getPushScope().isBlank()) {
            for (String part : levelConfig.getPushScope().split("[,，]")) {
                String receiver = part.trim();
                if (!receiver.isEmpty()) {
                    receivers.add(receiver);
                }
            }
        }
        if (receivers.isEmpty()) {
            receivers.add(DEFAULT_RECEIVER);
        }
        return receivers;
    }

    /**
     * 按等级选择推送渠道：红色电话 / 橙色短信 / 黄色应用消息 / 蓝色大屏。
     *
     * @param level 预警等级
     * @return 渠道编码
     */
    private String channelOf(WarningLevel level) {
        return switch (level) {
            case RED -> "voice";
            case ORANGE -> "sms";
            case YELLOW -> "app";
            case BLUE -> "screen";
        };
    }

    /**
     * 构建推送内容文案（含等级名称、标题与响应要求）。
     *
     * @param event       预警事件
     * @param level       预警等级
     * @param levelConfig 等级配置
     * @return 推送内容
     */
    private String buildContent(WarningEvent event, WarningLevel level, WarningLevelEntity levelConfig) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(level.getDescription()).append("】")
                .append(event.getTitle() == null ? event.getEventCode() : event.getTitle())
                .append("（编号 ").append(event.getEventCode()).append("）");
        if (levelConfig != null && levelConfig.getResponseRequire() != null) {
            sb.append("。响应要求：").append(levelConfig.getResponseRequire());
        }
        if (event.getSource() != null && !WarningSource.MANUAL.equals(event.getSource())) {
            sb.append("。来源：").append(event.getSource());
        }
        return sb.toString();
    }
}
