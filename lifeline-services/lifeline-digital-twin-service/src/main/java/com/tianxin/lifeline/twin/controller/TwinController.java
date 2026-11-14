package com.tianxin.lifeline.twin.controller;

import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.twin.dto.ModelDataVO;
import com.tianxin.lifeline.twin.dto.RealtimeMappingRequest;
import com.tianxin.lifeline.twin.dto.RealtimeMappingVO;
import com.tianxin.lifeline.twin.dto.SimulationRequest;
import com.tianxin.lifeline.twin.dto.SimulationResultVO;
import com.tianxin.lifeline.twin.dto.SimulationSubmitVO;
import com.tianxin.lifeline.twin.dto.WarningLocationResultVO;
import com.tianxin.lifeline.twin.service.TwinModelDataService;
import com.tianxin.lifeline.twin.service.TwinRealtimeMappingService;
import com.tianxin.lifeline.twin.service.TwinSimulationService;
import com.tianxin.lifeline.twin.service.TwinWarningLocationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数字孪生聚合 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/twin，与网关路由
 * /api/v1/twin/** → lb://digital-twin-service 对齐。
 * Controller 只做参数校验与 Result 包装，聚合编排全部下沉服务层；
 * 四个聚合接口均带降级能力，单依赖失败不产生 5xx。
 */
@Validated
@RestController
@RequestMapping("/api/v1/twin")
public class TwinController {

    /** 模型数据聚合服务 */
    private final TwinModelDataService modelDataService;

    /** 实时映射服务 */
    private final TwinRealtimeMappingService realtimeMappingService;

    /** 预警定位服务 */
    private final TwinWarningLocationService warningLocationService;

    /** 仿真推演服务 */
    private final TwinSimulationService simulationService;

    /**
     * 构造控制器（构造器注入）。
     *
     * @param modelDataService       模型数据聚合服务
     * @param realtimeMappingService 实时映射服务
     * @param warningLocationService 预警定位服务
     * @param simulationService      仿真推演服务
     */
    public TwinController(TwinModelDataService modelDataService,
                          TwinRealtimeMappingService realtimeMappingService,
                          TwinWarningLocationService warningLocationService,
                          TwinSimulationService simulationService) {
        this.modelDataService = modelDataService;
        this.realtimeMappingService = realtimeMappingService;
        this.warningLocationService = warningLocationService;
        this.simulationService = simulationService;
    }

    /**
     * 模型数据聚合（管线/设备/井盖统计 + GeoJSON 概要，Caffeine L1 缓存 30s，
     * Feign 失败降级返回带 degraded 标记的空骨架）。
     *
     * @param pipeType 管网类型（可空表示全部类型）
     * @param areaId   区域ID（可空表示全部区域）
     * @return 模型数据聚合视图
     */
    @GetMapping("/model-data")
    public Result<ModelDataVO> modelData(
            @RequestParam(value = "pipeType", required = false) String pipeType,
            @RequestParam(value = "areaId", required = false) Long areaId) {
        return Result.success(modelDataService.getModelData(pipeType, areaId));
    }

    /**
     * 实时映射（deviceId 列表或 areaId 二选一；Redis Pipeline 批量读取
     * rt:{deviceId}，单批上限 500 超限分页，单键失败跳过不中断）。
     *
     * @param request 实时映射请求
     * @return 实时映射聚合结果
     */
    @PostMapping("/realtime-mapping")
    public Result<RealtimeMappingVO> realtimeMapping(@Valid @RequestBody RealtimeMappingRequest request) {
        return Result.success(realtimeMappingService.mapping(request));
    }

    /**
     * 预警定位（按预警ID、管线活跃预警或区域活跃预警拼装位置与实时值，
     * 结果 Redis 缓存 1 分钟，缓存 scope 分别为 warning-{id}/pipe-{id}/area-{id}）。
     *
     * @param warningId 预警事件ID（与 pipeId/areaId 三选一）
     * @param pipeId    管网ID（与 warningId/areaId 三选一）
     * @param areaId    区域ID（与 warningId/pipeId 三选一）
     * @return 预警定位聚合结果
     */
    @GetMapping("/warning-location")
    public Result<WarningLocationResultVO> warningLocation(
            @RequestParam(value = "warningId", required = false) Long warningId,
            @RequestParam(value = "pipeId", required = false) Long pipeId,
            @RequestParam(value = "areaId", required = false) Long areaId) {
        return Result.success(warningLocationService.locate(warningId, pipeId, areaId));
    }

    /**
     * 提交仿真推演任务（立即返回 taskId，异步线程池执行规则推演，不阻塞请求）。
     *
     * @param request 场景类型与参数
     * @return 任务ID与初始状态
     */
    @PostMapping("/simulation")
    public Result<SimulationSubmitVO> submitSimulation(@Valid @RequestBody SimulationRequest request) {
        return Result.success(simulationService.submit(request));
    }

    /**
     * 轮询仿真推演任务状态与结果（任务状态 Redis 承载，TTL 30 分钟）。
     *
     * @param taskId 任务ID
     * @return 任务状态与结构化结果
     */
    @GetMapping("/simulation/{taskId}")
    public Result<SimulationResultVO> querySimulation(@PathVariable("taskId") String taskId) {
        return Result.success(simulationService.query(taskId));
    }
}
