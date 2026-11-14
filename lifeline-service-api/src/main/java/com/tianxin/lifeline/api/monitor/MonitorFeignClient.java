package com.tianxin.lifeline.api.monitor;

import com.tianxin.lifeline.api.monitor.dto.SensorDataDTO;
import com.tianxin.lifeline.api.monitor.dto.SensorStatisticsDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 监测数据服务（monitor-data-service）Feign 客户端。
 *
 * <p>供其他微服务查询设备实时/历史/统计监测数据。
 *
 * <p><b>deviceId 语义约定</b>：全链路（Mock 上报、TDengine 标签 device_id、
 * Redis key、本契约）统一使用「设备编码字符串」（如 DEV-GAS-001），
 * 不使用 PostgreSQL pipe_device 表的数值主键 id。
 */
@FeignClient(name = "monitor-data-service", path = "/api/v1/monitor")
public interface MonitorFeignClient {

    /**
     * 获取设备实时数据（优先 Redis 缓存，miss 回源 TDengine 最新一条）。
     *
     * @param deviceId 设备编码（字符串，如 DEV-GAS-001）
     * @return 最新一条传感数据
     */
    @GetMapping("/realtime/{deviceId}")
    Result<SensorDataDTO> getRealtimeData(@PathVariable("deviceId") String deviceId);

    /**
     * 查询设备历史数据。
     *
     * <p>传入 interval 时执行 TDengine INTERVAL 聚合查询（各指标取窗口均值，
     * ts 为窗口起始时间）；不传时返回原始明细数据。
     *
     * @param deviceId  设备编码（字符串）
     * @param startTime 开始时间（毫秒时间戳，含）
     * @param endTime   结束时间（毫秒时间戳，含）
     * @param interval  聚合间隔（可选），TDengine 时间单位格式，如 5m / 1h / 1d
     * @param limit     最大返回条数（服务端上限 10000）
     * @return 传感数据列表
     */
    @GetMapping("/history")
    Result<List<SensorDataDTO>> getHistoryData(@RequestParam("deviceId") String deviceId,
                                               @RequestParam("startTime") Long startTime,
                                               @RequestParam("endTime") Long endTime,
                                               @RequestParam(value = "interval", required = false) String interval,
                                               @RequestParam(value = "limit", defaultValue = "100") Integer limit);

    /**
     * 查询设备统计聚合数据（按小时/天粒度，TDengine INTERVAL 聚合）。
     *
     * @param deviceId  设备编码（字符串）
     * @param startTime 开始时间（毫秒时间戳，含）
     * @param endTime   结束时间（毫秒时间戳，不含）
     * @param interval  统计粒度，仅支持小时/天，如 1h / 6h / 1d
     * @return 各时间窗口的 max/min/avg 统计列表
     */
    @GetMapping("/statistics")
    Result<List<SensorStatisticsDTO>> getStatistics(@RequestParam("deviceId") String deviceId,
                                                    @RequestParam("startTime") Long startTime,
                                                    @RequestParam("endTime") Long endTime,
                                                    @RequestParam("interval") String interval);
}
