package com.tianxin.lifeline.monitor.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * WebSocket 实时推送 Actuator 健康指示器（P10-3 运维可观测）。
 *
 * <p>暴露至 {@code /actuator/health} 端点，综合判定 WebSocket 子系统状态：
 * <ul>
 *   <li><b>UP</b>：推送开关启用且最近有活跃会话或正常推送</li>
 *   <li><b>DOWN</b>：推送开关启用但鉴权失败率异常高（失败 &gt; 成功 × 3 且总鉴权 &gt; 10）</li>
 *   <li><b>UNKNOWN</b>：推送开关已停用</li>
 * </ul>
 *
 * <p>详细指标（当前会话数 / 累计推送 / 累计心跳 / 最后推送时间等）
 * 写入 Health detail，供运维面板消费。
 */
@Slf4j
@Component
public class MonitorWebSocketHealthIndicator implements HealthIndicator {

    /** 鉴权失败率异常阈值：失败次数超过成功次数的倍数 */
    private static final int AUTH_FAILURE_RATIO_THRESHOLD = 3;

    /** 鉴权异常判定最小样本数（避免少量请求时误判） */
    private static final long AUTH_MIN_SAMPLE_SIZE = 10;

    /** WebSocket 配置属性 */
    private final MonitorWebSocketProperties properties;

    /** 运行指标收集器 */
    private final MonitorWebSocketMetrics metrics;

    /**
     * 构造健康指示器。
     *
     * @param properties WebSocket 配置属性
     * @param metrics    运行指标收集器
     */
    public MonitorWebSocketHealthIndicator(MonitorWebSocketProperties properties,
                                           MonitorWebSocketMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 健康检查。
     *
     * @return Health 实例（UP / DOWN / UNKNOWN）
     */
    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.unknown()
                    .withDetail("status", "DISABLED")
                    .withDetail("message", "WebSocket 实时推送已停用")
                    .build();
        }

        // 构建详细指标
        Health.Builder builder = Health.up()
                .withDetail("currentSessions", metrics.getCurrentSessions())
                .withDetail("totalConnections", metrics.getTotalConnections())
                .withDetail("totalDisconnections", metrics.getTotalDisconnections())
                .withDetail("messagesPushed", metrics.getMessagesPushed())
                .withDetail("heartbeats", metrics.getHeartbeats())
                .withDetail("authSuccess", metrics.getAuthSuccessCount())
                .withDetail("authFailure", metrics.getAuthFailureCount())
                .withDetail("lastPushTimestamp", metrics.getLastPushTimestamp())
                .withDetail("lastHeartbeatTimestamp", metrics.getLastHeartbeatTimestamp())
                .withDetail("startTime", metrics.getStartTime().toString());

        // 鉴权异常检测：失败次数远大于成功次数，可能存在攻击或配置错误
        long authSuccess = metrics.getAuthSuccessCount();
        long authFailure = metrics.getAuthFailureCount();
        long totalAuth = authSuccess + authFailure;
        if (totalAuth >= AUTH_MIN_SAMPLE_SIZE && authFailure > authSuccess * AUTH_FAILURE_RATIO_THRESHOLD) {
            log.warn("WebSocket 健康告警: 鉴权失败率异常高, success={}, failure={}",
                    authSuccess, authFailure);
            builder.down()
                    .withDetail("alert", "AUTH_FAILURE_RATE_ABNORMAL")
                    .withDetail("message", "鉴权失败率异常，请检查客户端配置或是否存在攻击行为");
            return builder.build();
        }

        builder.withDetail("status", "RUNNING");
        return builder.build();
    }
}
