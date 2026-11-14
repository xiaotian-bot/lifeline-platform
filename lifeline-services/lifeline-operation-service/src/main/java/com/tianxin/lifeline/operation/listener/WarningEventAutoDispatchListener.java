package com.tianxin.lifeline.operation.listener;

import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.mq.envelope.EventEnvelope;
import com.tianxin.lifeline.operation.config.OperationProperties;
import com.tianxin.lifeline.operation.constant.OperationRedisKeys;
import com.tianxin.lifeline.operation.constant.OrderSource;
import com.tianxin.lifeline.operation.constant.OrderType;
import com.tianxin.lifeline.operation.dto.WarningEventPayloadDTO;
import com.tianxin.lifeline.operation.service.OperationOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 预警事件自动派单 Kafka 监听器。
 *
 * <p>订阅 {@link TopicConstants#PIPE_WARNING_EVENT}（Topic 名引用常量，禁止硬编码），
 * 使用本服务专用容器工厂 {@code warningAutoDispatchContainerFactory}
 * （EventEnvelope 信封 + MANUAL_IMMEDIATE 手动提交 + 重试错误处理器）消费预警事件，
 * 对橙色及以上等级（ORANGE / RED）预警自动创建应急工单：
 * 同进程直接调用 {@link OperationOrderService#createOrder}，
 * source 置 WARNING，标题/描述关联预警编号（warningNo）便于追溯。
 *
 * <p>幂等：Redis SETNX（eventId 级，仿 warning-service 双重保障思路），
 * 防重复建单；处理成功才 ack，失败先删幂等键再上抛（交由容器错误处理器重试）。
 *
 * <p>时效过滤：新消费组 earliest 回放历史预警时，预警时间早于当前超过
 * {@code max-age-minutes}（默认 30 分钟）的事件直接 ack 跳过，避免批量建过期工单。
 *
 * <p>开关：{@code lifeline.operation.auto-dispatch.enabled}（默认 true）。
 *
 * <p>注意：warning-service 发布端在预警落库前投递事件，载荷不含预警事件主键，
 * 故工单 warning_event_id 暂无法填充，追溯以 warningNo 关联。
 */
@Slf4j
@Component
public class WarningEventAutoDispatchListener {

    /** 需要自动派单的预警等级（橙色及以上） */
    private static final Set<String> DISPATCH_LEVELS = Set.of("ORANGE", "RED");

    /** 红色预警工单优先级（1 最高） */
    private static final int PRIORITY_RED = 1;

    /** 橙色预警工单优先级 */
    private static final int PRIORITY_ORANGE = 2;

    /** 工单业务服务（同进程直调 service 层） */
    private final OperationOrderService operationOrderService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 运维业务配置（自动派单开关/幂等TTL） */
    private final OperationProperties operationProperties;

    /**
     * 构造监听器。
     *
     * @param operationOrderService 工单业务服务
     * @param stringRedisTemplate   Redis 模板
     * @param operationProperties   运维业务配置
     */
    public WarningEventAutoDispatchListener(OperationOrderService operationOrderService,
                                            StringRedisTemplate stringRedisTemplate,
                                            OperationProperties operationProperties) {
        this.operationOrderService = operationOrderService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.operationProperties = operationProperties;
    }

    /**
     * 消费预警事件信封并自动派单。
     *
     * <p>处理成功（含开关关闭/等级过滤/重复拦截等跳过路径）才 ack；
     * 建单失败先删除幂等键再上抛异常，保证重投递后可重试建单。
     *
     * @param envelope 事件信封
     * @param ack      手动偏移量提交句柄
     */
    @KafkaListener(topics = TopicConstants.PIPE_WARNING_EVENT,
            containerFactory = "warningAutoDispatchContainerFactory")
    public void onWarningEvent(EventEnvelope<Object> envelope, Acknowledgment ack) {
        String eventId = envelope == null ? null : envelope.getEventId();
        String idemKey = StringUtils.hasText(eventId)
                ? OperationRedisKeys.autoDispatchIdempotent(eventId) : null;
        try {
            handle(envelope, idemKey);
            ack.acknowledge();
        } catch (Exception e) {
            // 失败先删幂等键再上抛：避免占用幂等键导致重试无法建单；
            // 删键自身异常仅告警，绝不吞掉原始异常
            if (idemKey != null) {
                try {
                    stringRedisTemplate.delete(idemKey);
                } catch (Exception deleteEx) {
                    log.error("删除自动派单幂等键失败(重试仍可能被拦截): idemKey={}", idemKey, deleteEx);
                }
            }
            throw e;
        }
    }

    /**
     * 处理单个预警事件信封：开关 → 幂等 → 负载转换 → 等级过滤 → 建单。
     *
     * @param envelope 事件信封
     * @param idemKey  幂等键（eventId 为空时为 null，退化为无幂等直建）
     */
    private void handle(EventEnvelope<Object> envelope, String idemKey) {
        if (!operationProperties.getAutoDispatch().isEnabled()) {
            log.debug("预警自动派单已关闭(lifeline.operation.auto-dispatch.enabled=false), 跳过");
            return;
        }
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("收到空预警事件信封, 已跳过");
            return;
        }

        // 幂等：Redis SETNX（eventId 级，仿 warning-service），先拦重复投递
        if (idemKey != null) {
            Duration ttl = Duration.ofMinutes(operationProperties.getAutoDispatch().getIdempotentTtlMinutes());
            Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", ttl);
            if (!Boolean.TRUE.equals(firstSeen)) {
                log.debug("预警事件重复投递已拦截, 跳过自动派单: eventId={}", envelope.getEventId());
                return;
            }
        }

        WarningEventPayloadDTO payload = envelope.convertPayload(WarningEventPayloadDTO.class);
        if (payload == null) {
            log.warn("预警事件负载转换失败, 已跳过: eventId={}", envelope.getEventId());
            return;
        }

        // 时效过滤：新消费组 earliest 回放历史预警时，过期事件直接跳过，避免批量建过期工单
        long maxAgeMinutes = operationProperties.getAutoDispatch().getMaxAgeMinutes();
        if (maxAgeMinutes > 0 && payload.getWarningTime() != null
                && payload.getWarningTime().isBefore(LocalDateTime.now().minusMinutes(maxAgeMinutes))) {
            log.warn("预警事件已过期, 跳过自动派单: eventId={}, warningNo={}, warningTime={}, 时效阈值={}分钟",
                    envelope.getEventId(), payload.getWarningNo(), payload.getWarningTime(), maxAgeMinutes);
            return;
        }

        // 等级过滤：仅橙色及以上（ORANGE / RED）自动派单
        String level = payload.getWarningLevel() == null ? "" : payload.getWarningLevel().toUpperCase();
        if (!DISPATCH_LEVELS.contains(level)) {
            log.debug("预警等级低于橙色, 不自动派单: eventId={}, level={}", envelope.getEventId(), level);
            return;
        }

        Long orderId = operationOrderService.createOrder(buildOrder(payload, level));
        log.info("预警自动派单成功: eventId={}, warningNo={}, level={}, orderId={}",
                envelope.getEventId(), payload.getWarningNo(), level, orderId);
    }

    /**
     * 由预警负载构建应急工单创建参数。
     *
     * @param payload 预警负载
     * @param level   归一化后的预警等级（ORANGE / RED）
     * @return 工单创建参数
     */
    private OrderCreateDTO buildOrder(WarningEventPayloadDTO payload, String level) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setOrderType(OrderType.EMERGENCY);
        dto.setSource(OrderSource.WARNING);
        dto.setPriority("RED".equals(level) ? PRIORITY_RED : PRIORITY_ORANGE);
        dto.setPipeType(payload.getPipeType());
        dto.setPipeId(payload.getPipeId());
        dto.setDeviceId(payload.getDeviceId());
        dto.setAreaId(payload.getAreaId());
        dto.setAssigneeId(payload.getAssigneeId());
        dto.setTitle("【预警自动派单】" + (StringUtils.hasText(payload.getTitle())
                ? payload.getTitle() : "橙色及以上预警应急处置"));
        dto.setDescription(buildDescription(payload, level));
        return dto;
    }

    /**
     * 拼接工单描述（附预警编号/等级/触发值，供追溯关联预警事件）。
     *
     * @param payload 预警负载
     * @param level   预警等级
     * @return 工单描述
     */
    private String buildDescription(WarningEventPayloadDTO payload, String level) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(payload.getDescription())) {
            sb.append(payload.getDescription());
        }
        sb.append(" | 预警编号: ").append(payload.getWarningNo() == null ? "-" : payload.getWarningNo());
        sb.append(" | 预警等级: ").append(level);
        if (payload.getMetricType() != null && payload.getTriggerValue() != null) {
            sb.append(" | 指标: ").append(payload.getMetricType())
                    .append(" 触发值: ").append(payload.getTriggerValue());
            if (payload.getThreshold() != null) {
                sb.append(" 阈值: ").append(payload.getThreshold());
            }
        }
        return sb.toString();
    }
}
