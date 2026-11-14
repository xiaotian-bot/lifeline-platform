package com.tianxin.lifeline.mock.controller;

import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.mock.config.MockProperties;
import com.tianxin.lifeline.mock.model.DeviceInfo;
import com.tianxin.lifeline.mock.scenario.AnomalyScenario;
import com.tianxin.lifeline.mock.scenario.ScenarioManager;
import com.tianxin.lifeline.mock.scheduler.DataReportScheduler;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock 控制台 REST 接口。
 *
 * <p>提供数据生成的启停控制、状态查询、异常场景注入/清除与动态调参能力，
 * 便于联调与压测期间人工操控模拟数据源。
 */
@RestController
@RequestMapping("/api/v1/mock")
public class MockController {

    /** 上报调度器 */
    private final DataReportScheduler scheduler;

    /** 异常场景管理器 */
    private final ScenarioManager scenarioManager;

    /** Mock 配置 */
    private final MockProperties properties;

    /**
     * 构造控制台。
     *
     * @param scheduler       上报调度器
     * @param scenarioManager 异常场景管理器
     * @param properties      Mock 配置
     */
    public MockController(DataReportScheduler scheduler,
                          ScenarioManager scenarioManager,
                          MockProperties properties) {
        this.scheduler = scheduler;
        this.scenarioManager = scenarioManager;
        this.properties = properties;
    }

    /**
     * 启动数据生成。
     *
     * @return 启动结果
     */
    @PostMapping("/start")
    public Result<Map<String, Object>> start() {
        boolean started = scheduler.start();
        return Result.success(Map.of(
                "started", started,
                "message", started ? "数据生成已启动" : "数据生成已在运行中"));
    }

    /**
     * 停止数据生成。
     *
     * @return 停止结果
     */
    @PostMapping("/stop")
    public Result<Map<String, Object>> stop() {
        boolean stopped = scheduler.stop();
        return Result.success(Map.of(
                "stopped", stopped,
                "message", stopped ? "数据生成已停止" : "数据生成未在运行"));
    }

    /**
     * 查看当前状态（运行状态、设备数、上报统计、激活异常场景）。
     *
     * @return 状态信息
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>(scheduler.status());
        Map<String, Object> scenarios = new LinkedHashMap<>();
        scenarioManager.listActive().forEach((deviceId, active) ->
                scenarios.put(deviceId, Map.of(
                        "scenario", active.scenario().getCode(),
                        "startTime", active.startTime(),
                        "endTime", active.endTime())));
        status.put("activeScenarios", scenarios);
        return Result.success(status);
    }

    /**
     * 列出全部支持的异常场景（含编码、描述与默认持续时长）。
     *
     * @return 场景清单
     */
    @GetMapping("/scenario/types")
    public Result<List<Map<String, Object>>> listScenarioTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (AnomalyScenario scenario : ScenarioManager.supportedScenarios()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", scenario.getCode());
            item.put("description", scenario.getDescription());
            item.put("defaultDurationSeconds", scenario.getDefaultDurationSeconds());
            types.add(item);
        }
        return Result.success(types);
    }

    /**
     * 触发异常场景。
     *
     * @param request 触发请求（deviceId 必填，scenarioType 必填，durationSeconds 可选）
     * @return 触发结果
     */
    @PostMapping("/scenario/trigger")
    public Result<Map<String, Object>> triggerScenario(@RequestBody ScenarioTriggerRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            return Result.fail("deviceId 不能为空");
        }
        AnomalyScenario scenario = AnomalyScenario.fromCode(request.getScenarioType());
        if (scenario == null) {
            return Result.fail("无效的 scenarioType，支持: " + supportedScenarioCodes());
        }
        if (DeviceInfo.parsePipeType(request.getDeviceId()) == null) {
            return Result.fail("设备ID格式不符合规范 DEV-{GAS|WAT|DRN|HTG|STR}-{三位序号}");
        }
        if (request.getDurationSeconds() != null && request.getDurationSeconds() > 0) {
            scenarioManager.trigger(request.getDeviceId(), scenario, request.getDurationSeconds());
        } else {
            scenarioManager.trigger(request.getDeviceId(), scenario);
        }
        return Result.success(Map.of(
                "deviceId", request.getDeviceId(),
                "scenario", scenario.getCode(),
                "description", scenario.getDescription()));
    }

    /**
     * 清除异常场景（指定设备，或 deviceId 为空时清除全部）。
     *
     * @param request 清除请求（deviceId 可选）
     * @return 清除结果
     */
    @PostMapping("/scenario/clear")
    public Result<Map<String, Object>> clearScenario(@RequestBody(required = false) ScenarioClearRequest request) {
        if (request == null || request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            int cleared = scenarioManager.clearAll();
            return Result.success(Map.of("clearedCount", cleared, "scope", "ALL"));
        }
        boolean cleared = scenarioManager.clear(request.getDeviceId());
        return Result.success(Map.of(
                "deviceId", request.getDeviceId(),
                "cleared", cleared,
                "scope", "DEVICE"));
    }

    /**
     * 动态调整配置（设备数量、上报频率），立即生效。
     *
     * @param request 配置更新请求
     * @return 更新后的关键配置
     */
    @PutMapping("/config")
    public Result<Map<String, Object>> updateConfig(@RequestBody ConfigUpdateRequest request) {
        scheduler.updateConfig(request.getDeviceCount(), request.getReportIntervalSeconds());
        return Result.success(Map.of(
                "deviceCount", properties.getSensor().getCount(),
                "reportIntervalSeconds", properties.getSensor().getReportIntervalSeconds()));
    }

    /**
     * 异常场景触发请求体。
     */
    @Data
    public static class ScenarioTriggerRequest {

        /** 目标设备ID，如 DEV-GAS-001 */
        private String deviceId;

        /** 场景类型：PRESSURE_DROP / GAS_LEAK / TEMP_SPIKE / HEATING_LEAK /
         * DRAINAGE_OVERFLOW / THIRD_PARTY_DAMAGE */
        private String scenarioType;

        /** 场景持续时长（秒），缺省使用场景默认时长 */
        private Integer durationSeconds;
    }

    /**
     * 异常场景清除请求体。
     */
    @Data
    public static class ScenarioClearRequest {

        /** 目标设备ID，为空表示清除全部 */
        private String deviceId;
    }

    /**
     * 动态配置更新请求体。
     */
    @Data
    public static class ConfigUpdateRequest {

        /** 新设备数量 */
        private Integer deviceCount;

        /** 新上报周期（秒） */
        private Integer reportIntervalSeconds;
    }

    /**
     * 拼接全部支持的场景编码（用于错误提示）。
     *
     * @return 场景编码列表字符串，如 PRESSURE_DROP / GAS_LEAK / ...
     */
    private static String supportedScenarioCodes() {
        StringBuilder sb = new StringBuilder();
        for (AnomalyScenario scenario : ScenarioManager.supportedScenarios()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(scenario.getCode());
        }
        return sb.toString();
    }
}
