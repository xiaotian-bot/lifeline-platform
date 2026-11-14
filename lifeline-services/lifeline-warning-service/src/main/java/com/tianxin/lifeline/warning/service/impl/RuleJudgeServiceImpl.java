package com.tianxin.lifeline.warning.service.impl;

import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.core.constant.WarningLevel;
import com.tianxin.lifeline.common.mq.template.KafkaEventTemplate;
import com.tianxin.lifeline.warning.config.WarningProperties;
import com.tianxin.lifeline.warning.constant.WarningRedisKeys;
import com.tianxin.lifeline.warning.constant.WarningSource;
import com.tianxin.lifeline.warning.dto.SensorRawMessage;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.entity.WarningRule;
import com.tianxin.lifeline.warning.service.RuleCacheService;
import com.tianxin.lifeline.warning.service.RuleJudgeService;
import com.tianxin.lifeline.warning.util.DeviceFieldUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 规则判定服务实现。
 *
 * <p>判定语义：遍历启用规则快照，规则的 rule_type 对应指标字段取值
 * 大于等于 threshold_value 即命中（pipe_type 为空表示不限管网类型）；
 * 多条规则命中时取风险最高（级别数值最小）的一条。
 *
 * <p>抑制逻辑：同设备同指标在 suppress-minutes（默认 10 分钟）窗口内
 * 只投递一次预警事件，基于 Redis SETNX 实现。
 *
 * <p>命中后以 EventEnvelope 信封（eventId 由信封自动以 UUID 生成，作为
 * 下游幂等键）投递 {@link TopicConstants#PIPE_WARNING_EVENT}。
 */
@Slf4j
@Service
public class RuleJudgeServiceImpl implements RuleJudgeService {

    /** 预警事件 Kafka 事件类型 */
    private static final String EVENT_TYPE_WARNING_EVENT = "WARNING_EVENT";

    /** 事件来源服务名 */
    private static final String EVENT_SOURCE = "warning-service";

    /** 规则缓存服务 */
    private final RuleCacheService ruleCacheService;

    /** Kafka 事件发送模板 */
    private final KafkaEventTemplate kafkaEventTemplate;

    /** Redis 模板（预警抑制窗口） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 预警业务配置 */
    private final WarningProperties warningProperties;

    /**
     * 构造规则判定服务。
     *
     * @param ruleCacheService    规则缓存服务
     * @param kafkaEventTemplate  Kafka 事件发送模板
     * @param stringRedisTemplate Redis 模板
     * @param warningProperties   预警业务配置
     */
    public RuleJudgeServiceImpl(RuleCacheService ruleCacheService,
                                KafkaEventTemplate kafkaEventTemplate,
                                StringRedisTemplate stringRedisTemplate,
                                WarningProperties warningProperties) {
        this.ruleCacheService = ruleCacheService;
        this.kafkaEventTemplate = kafkaEventTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.warningProperties = warningProperties;
    }

    /**
     * 对单条传感数据做规则越限判定并投递预警事件。
     *
     * @param message 传感数据
     */
    @Override
    public void judge(SensorRawMessage message) {
        WarningRule hitRule = null;
        Double hitValue = null;
        // 遍历启用规则，命中多条时保留风险最高（级别数值最小）的一条
        for (WarningRule rule : ruleCacheService.enabledRules()) {
            if (rule.getRuleType() == null || rule.getThresholdValue() == null) {
                continue;
            }
            if (!matchPipeType(rule, message)) {
                continue;
            }
            Double value = message.metricValue(rule.getRuleType());
            if (value == null) {
                continue;
            }
            boolean exceeded = value >= rule.getThresholdValue().doubleValue();
            if (!exceeded) {
                continue;
            }
            if (hitRule == null || higherSeverity(rule, hitRule)) {
                hitRule = rule;
                hitValue = value;
            }
        }
        if (hitRule == null) {
            return;
        }
        if (suppressed(message, hitRule)) {
            return;
        }
        dispatch(message, hitRule, hitValue);
    }

    /**
     * 判断规则的管网类型是否与消息匹配（规则未限定管网类型视为全匹配）。
     *
     * @param rule    预警规则
     * @param message 传感数据
     * @return true 表示匹配
     */
    private boolean matchPipeType(WarningRule rule, SensorRawMessage message) {
        if (rule.getPipeType() == null || rule.getPipeType().isBlank()) {
            return true;
        }
        return rule.getPipeType().equalsIgnoreCase(message.getPipeType());
    }

    /**
     * 判断 candidate 规则的风险是否高于 current（级别数值更小 = 风险更高）。
     *
     * @param candidate 候选规则
     * @param current   当前命中规则
     * @return true 表示候选风险更高
     */
    private boolean higherSeverity(WarningRule candidate, WarningRule current) {
        Integer cl = candidate.getWarningLevel();
        Integer ul = current.getWarningLevel();
        if (cl == null) {
            return false;
        }
        if (ul == null) {
            return true;
        }
        return cl < ul;
    }

    /**
     * 预警抑制判断：同设备同指标窗口内 SETNX 抢占，抢占失败表示已被抑制。
     *
     * @param message 传感数据
     * @param rule    命中的规则
     * @return true 表示处于抑制窗口内（不投递）
     */
    private boolean suppressed(SensorRawMessage message, WarningRule rule) {
        String key = WarningRedisKeys.suppress(message.getDeviceId(), rule.getRuleType());
        Duration window = Duration.ofMinutes(warningProperties.getSuppressMinutes());
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", window);
        if (!Boolean.TRUE.equals(acquired)) {
            log.debug("预警抑制生效, 窗口内不重复投递: deviceId={}, metric={}",
                    message.getDeviceId(), rule.getRuleType());
            return true;
        }
        return false;
    }

    /**
     * 构建预警负载并以事件信封投递 pipe_warning_event。
     *
     * @param message  传感数据
     * @param rule     命中的规则
     * @param hitValue 触发值
     */
    private void dispatch(SensorRawMessage message, WarningRule rule, Double hitValue) {
        WarningLevel level = WarningLevel.fromLevel(
                rule.getWarningLevel() == null ? WarningLevel.BLUE.getLevel() : rule.getWarningLevel());
        if (level == null) {
            level = WarningLevel.BLUE;
        }
        long ts = message.getTs();
        WarningEventPayload payload = new WarningEventPayload();
        payload.setWarningNo(buildWarningNo(message.getDeviceId(), rule.getRuleType(), ts));
        payload.setSource(WarningSource.RULE);
        payload.setWarningLevel(level.getCode());
        payload.setPipeType(message.getPipeType());
        payload.setDeviceId(DeviceFieldUtils.parseId(message.getDeviceId()));
        payload.setDeviceCode(message.getDeviceId());
        payload.setAreaId(DeviceFieldUtils.parseId(message.getAreaId()));
        payload.setTitle(String.format("【%s】设备%s %s越限", level.getDescription(),
                message.getDeviceId(), rule.getRuleType()));
        payload.setDescription(String.format("设备%s的%s指标实测值%.4f达到/超过阈值%.4f，命中规则「%s」",
                message.getDeviceId(), rule.getRuleType(), hitValue,
                rule.getThresholdValue().doubleValue(), rule.getRuleName()));
        payload.setMetricType(rule.getRuleType());
        payload.setTriggerValue(hitValue);
        payload.setThreshold(rule.getThresholdValue().doubleValue());
        payload.setRuleId(rule.getId());
        payload.setWarningType(rule.getRuleName());
        payload.setWarningTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()));

        // 分区键取设备ID，保证同设备预警有序；信封 eventId 自动生成 UUID 作为幂等键
        kafkaEventTemplate.sendEvent(TopicConstants.PIPE_WARNING_EVENT,
                message.getDeviceId(), EVENT_TYPE_WARNING_EVENT, EVENT_SOURCE, payload);
        log.info("规则命中投递预警事件: warningNo={}, level={}, deviceId={}, metric={}, value={}",
                payload.getWarningNo(), level.getCode(), message.getDeviceId(),
                rule.getRuleType(), hitValue);
    }

    /**
     * 生成预警编号：设备 + 指标 + 时间窗（与抑制窗口对齐，窗口内天然幂等）。
     *
     * @param deviceId   设备编码
     * @param metricType 指标类型
     * @param ts         数据时间戳（毫秒）
     * @return 预警编号
     */
    private String buildWarningNo(String deviceId, String metricType, long ts) {
        long windowMillis = Math.max(1L, warningProperties.getSuppressMinutes()) * 60L * 1000L;
        return "W-" + deviceId + "-" + metricType + "-" + (ts / windowMillis);
    }
}
