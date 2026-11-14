package com.tianxin.lifeline.monitor.controller;

import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.monitor.dto.SensorDataVO;
import com.tianxin.lifeline.monitor.dto.SensorStatisticsVO;
import com.tianxin.lifeline.monitor.service.MonitorQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监测数据查询控制器。
 *
 * <p>路径前缀 {@code /api/v1/monitor}，接口签名与 service-api 模块的
 * {@code MonitorFeignClient} 契约保持一致（本模块未依赖 service-api，
 * 响应 VO 与其 DTO 字段同构，JSON over HTTP 层面完全兼容）：
 * <ul>
 *     <li>GET /realtime/{deviceId} —— 设备实时数据（Redis 优先，TDengine 回源）；</li>
 *     <li>GET /history —— 历史明细 / INTERVAL 聚合查询；</li>
 *     <li>GET /statistics —— 小时/天粒度 max/min/avg 统计。</li>
 * </ul>
 *
 * <p>本控制器只做参数接收与 {@link Result} 包装，业务校验与查询逻辑
 * 全部下沉 Service；业务异常由公共模块全局异常处理器统一转换。
 */
@RestController
@RequestMapping("/api/v1/monitor")
public class MonitorDataController {

    /** 监测数据查询服务 */
    private final MonitorQueryService monitorQueryService;

    /**
     * 构造控制器。
     *
     * @param monitorQueryService 监测数据查询服务
     */
    public MonitorDataController(MonitorQueryService monitorQueryService) {
        this.monitorQueryService = monitorQueryService;
    }

    /**
     * 获取设备实时数据。
     *
     * @param deviceId 设备编码（字符串，如 DEV-GAS-001）
     * @return 最新一条传感数据
     */
    @GetMapping("/realtime/{deviceId}")
    public Result<SensorDataVO> getRealtimeData(@PathVariable("deviceId") String deviceId) {
        return Result.success(monitorQueryService.getRealtime(deviceId));
    }

    /**
     * 查询设备历史数据（interval 非空时为 TDengine INTERVAL 聚合查询）。
     *
     * @param deviceId  设备编码
     * @param startTime 开始时间（毫秒时间戳，含）
     * @param endTime   结束时间（毫秒时间戳，含）
     * @param interval  聚合间隔（可选），如 5m / 1h / 1d
     * @param limit     最大返回条数（默认 100，服务端上限 10000）
     * @return 传感数据列表
     */
    @GetMapping("/history")
    public Result<List<SensorDataVO>> getHistoryData(@RequestParam("deviceId") String deviceId,
                                                     @RequestParam("startTime") Long startTime,
                                                     @RequestParam("endTime") Long endTime,
                                                     @RequestParam(value = "interval", required = false) String interval,
                                                     @RequestParam(value = "limit", defaultValue = "100") Integer limit) {
        return Result.success(monitorQueryService.queryHistory(deviceId, startTime, endTime, interval, limit));
    }

    /**
     * 查询设备统计聚合数据（小时/天粒度）。
     *
     * @param deviceId  设备编码
     * @param startTime 开始时间（毫秒时间戳，含）
     * @param endTime   结束时间（毫秒时间戳，不含）
     * @param interval  统计粒度，仅支持 h/d，如 1h / 6h / 1d
     * @return 各时间窗口 max/min/avg 统计列表
     */
    @GetMapping("/statistics")
    public Result<List<SensorStatisticsVO>> getStatistics(@RequestParam("deviceId") String deviceId,
                                                          @RequestParam("startTime") Long startTime,
                                                          @RequestParam("endTime") Long endTime,
                                                          @RequestParam("interval") String interval) {
        return Result.success(monitorQueryService.queryStatistics(deviceId, startTime, endTime, interval));
    }

    /**
     * 查询设备传感器数据（简化接口，返回最近 N 条数据）。
     *
     * @param deviceId 设备编码
     * @param limit    最大返回条数（默认 20）
     * @return 传感数据列表
     */
    @GetMapping("/sensor-data")
    public Result<List<SensorDataVO>> getSensorData(@RequestParam("deviceId") String deviceId,
                                                     @RequestParam(value = "limit", defaultValue = "20") Integer limit) {
        return Result.success(monitorQueryService.querySensorData(deviceId, limit));
    }
}
