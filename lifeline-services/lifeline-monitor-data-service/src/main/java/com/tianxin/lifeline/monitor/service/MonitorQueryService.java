package com.tianxin.lifeline.monitor.service;

import com.tianxin.lifeline.monitor.dto.SensorDataVO;
import com.tianxin.lifeline.monitor.dto.SensorStatisticsVO;

import java.util.List;

/**
 * 监测数据查询服务。
 *
 * <p>提供实时（Redis 优先 + TDengine 回源）、历史（明细/INTERVAL 聚合）、
 * 统计（小时/天粒度 max/min/avg）三类查询能力。
 * deviceId 一律为设备编码字符串（如 DEV-GAS-001）。
 */
public interface MonitorQueryService {

    /**
     * 查询设备实时数据：优先读取 Redis 缓存 {@code rt:{deviceId}}，
     * miss 时回源 TDengine 最新一条。
     *
     * @param deviceId 设备编码
     * @return 最新传感数据
     */
    SensorDataVO getRealtime(String deviceId);

    /**
     * 查询设备历史数据。
     *
     * @param deviceId  设备编码
     * @param startTime 开始时间（毫秒，含）
     * @param endTime   结束时间（毫秒，含）
     * @param interval  聚合间隔（可空，如 5m/1h；为空返回原始明细）
     * @param limit     最大返回条数（服务端上限 10000）
     * @return 传感数据列表
     */
    List<SensorDataVO> queryHistory(String deviceId, Long startTime, Long endTime, String interval, Integer limit);

    /**
     * 查询设备统计聚合数据（小时/天粒度）。
     *
     * @param deviceId  设备编码
     * @param startTime 开始时间（毫秒，含）
     * @param endTime   结束时间（毫秒，不含）
     * @param interval  统计粒度（仅支持 h/d，如 1h/1d）
     * @return 各时间窗口统计列表
     */
    List<SensorStatisticsVO> queryStatistics(String deviceId, Long startTime, Long endTime, String interval);

    /**
     * 查询设备最近传感器数据（简化接口，无需时间范围）。
     *
     * @param deviceId 设备编码
     * @param limit    最大返回条数（默认 20）
     * @return 传感数据列表
     */
    List<SensorDataVO> querySensorData(String deviceId, Integer limit);
}
