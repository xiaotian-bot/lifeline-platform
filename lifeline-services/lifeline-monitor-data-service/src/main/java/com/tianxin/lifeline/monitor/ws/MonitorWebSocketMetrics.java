package com.tianxin.lifeline.monitor.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * WebSocket 实时推送运行指标收集器（P10-3 运维可观测）。
 *
 * <p>以线程安全计数器记录连接/断开/鉴权成功失败/消息推送/心跳等事件，
 * 供 {@link MonitorWebSocketHealthIndicator} 读取并暴露至 Actuator 健康端点。
 *
 * <p>所有计数器均为进程级（单副本），跨副本需聚合各实例指标。
 */
@Slf4j
@Component
public class MonitorWebSocketMetrics {

    /** 累计连接成功次数 */
    private final LongAdder totalConnections = new LongAdder();

    /** 累计断开连接次数 */
    private final LongAdder totalDisconnections = new LongAdder();

    /** 累计鉴权成功次数 */
    private final LongAdder authSuccessCount = new LongAdder();

    /** 累计鉴权失败次数 */
    private final LongAdder authFailureCount = new LongAdder();

    /** 累计消息推送条数（成功发送至客户端） */
    private final LongAdder messagesPushed = new LongAdder();

    /** 累计心跳次数 */
    private final LongAdder heartbeats = new LongAdder();

    /** 当前活跃会话数（连接+1 / 断开-1，AtomicLong 保证原子性） */
    private final AtomicLong currentSessions = new AtomicLong(0);

    /** 最后一次消息推送时间戳（毫秒） */
    private final AtomicLong lastPushTimestamp = new AtomicLong(0);

    /** 最后一次心跳时间戳（毫秒） */
    private final AtomicLong lastHeartbeatTimestamp = new AtomicLong(0);

    /** 指标采集启动时间 */
    private final Instant startTime = Instant.now();

    // ---- 连接/断开 ----

    /** 记录一次连接建立。 */
    public void recordConnection() {
        totalConnections.increment();
        currentSessions.incrementAndGet();
    }

    /** 记录一次连接断开。 */
    public void recordDisconnection() {
        totalDisconnections.increment();
        currentSessions.decrementAndGet();
    }

    // ---- 鉴权 ----

    /** 记录一次鉴权成功。 */
    public void recordAuthSuccess() {
        authSuccessCount.increment();
    }

    /** 记录一次鉴权失败。 */
    public void recordAuthFailure() {
        authFailureCount.increment();
    }

    // ---- 消息推送 ----

    /** 记录一条消息推送。 */
    public void recordMessagePushed() {
        messagesPushed.increment();
        lastPushTimestamp.set(System.currentTimeMillis());
    }

    // ---- 心跳 ----

    /** 记录一次心跳。 */
    public void recordHeartbeat() {
        heartbeats.increment();
        lastHeartbeatTimestamp.set(System.currentTimeMillis());
    }

    // ---- 查询指标 ----

    /** @return 累计连接次数 */
    public long getTotalConnections() {
        return totalConnections.sum();
    }

    /** @return 累计断开次数 */
    public long getTotalDisconnections() {
        return totalDisconnections.sum();
    }

    /** @return 当前活跃会话数 */
    public long getCurrentSessions() {
        return currentSessions.get();
    }

    /** @return 累计鉴权成功次数 */
    public long getAuthSuccessCount() {
        return authSuccessCount.sum();
    }

    /** @return 累计鉴权失败次数 */
    public long getAuthFailureCount() {
        return authFailureCount.sum();
    }

    /** @return 累计消息推送条数 */
    public long getMessagesPushed() {
        return messagesPushed.sum();
    }

    /** @return 累计心跳次数 */
    public long getHeartbeats() {
        return heartbeats.sum();
    }

    /** @return 最后一次推送时间戳（毫秒），无推送返回 0 */
    public long getLastPushTimestamp() {
        return lastPushTimestamp.get();
    }

    /** @return 最后一次心跳时间戳（毫秒），无心跳返回 0 */
    public long getLastHeartbeatTimestamp() {
        return lastHeartbeatTimestamp.get();
    }

    /** @return 指标采集启动时刻 */
    public Instant getStartTime() {
        return startTime;
    }
}
