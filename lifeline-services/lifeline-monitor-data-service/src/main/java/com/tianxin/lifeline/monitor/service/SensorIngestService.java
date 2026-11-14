package com.tianxin.lifeline.monitor.service;

import com.tianxin.lifeline.monitor.dto.SensorRawMessage;

import java.util.List;

/**
 * 传感数据接入服务。
 *
 * <p>负责将 Kafka 消费到的原始传感数据写入数据链路：
 * TDengine 时序攒批写入、PostgreSQL 设备状态回写、Redis 实时缓存刷新。
 */
public interface SensorIngestService {

    /**
     * 接入单条传感数据（内部完成 pipeType 归一化与三路写入分发）。
     *
     * @param message 已通过 deviceId/ts 非空校验的传感数据
     */
    void ingest(SensorRawMessage message);

    /**
     * 批量接入传感数据。
     *
     * @param messages 已通过校验的传感数据列表
     */
    void ingestBatch(List<SensorRawMessage> messages);
}
