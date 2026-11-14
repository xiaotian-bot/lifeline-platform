package com.tianxin.lifeline.warning.controller;

import com.tianxin.lifeline.api.warning.dto.WarningEventDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.warning.dto.WarningEventVO;
import com.tianxin.lifeline.warning.dto.WarningQueryRequest;
import com.tianxin.lifeline.warning.dto.WarningStatisticsVO;
import com.tianxin.lifeline.warning.service.WarningEventService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 预警事件 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/warnings，与 WarningFeignClient 契约对齐
 * （getWarningById / listActiveWarnings / createWarning 签名一致），
 * Controller 只做参数校验与 Result 包装，业务逻辑全部下沉服务层。
 */
@RestController
@RequestMapping("/api/v1/warnings")
public class WarningController {

    /** 预警事件业务服务 */
    private final WarningEventService warningEventService;

    /**
     * 构造控制器。
     *
     * @param warningEventService 预警事件业务服务
     */
    public WarningController(WarningEventService warningEventService) {
        this.warningEventService = warningEventService;
    }

    /**
     * 分页查询预警事件。
     *
     * @param request 分页与过滤条件（等级/状态/管网类型/管网ID/设备ID/时间范围）
     * @return 分页结果（状态输出中文）
     */
    @GetMapping
    public Result<PageResult<WarningEventVO>> page(@Valid WarningQueryRequest request) {
        return Result.success(warningEventService.page(request));
    }

    /**
     * 按等级/管网类型/状态统计预警事件。
     *
     * @return 统计结果
     */
    @GetMapping("/statistics")
    public Result<WarningStatisticsVO> statistics() {
        return Result.success(warningEventService.statistics());
    }

    /**
     * 查询指定管网的活跃预警列表（契约 listActiveWarnings）。
     *
     * @param pipeId 管网ID
     * @return 活跃预警列表
     */
    @GetMapping("/active")
    public Result<List<WarningEventDTO>> listActiveWarnings(@RequestParam("pipeId") Long pipeId) {
        return Result.success(warningEventService.listActive(pipeId));
    }

    /**
     * 查询指定区域的活跃预警列表（契约 listActiveWarningsByArea，按 area_id 过滤）。
     *
     * @param areaId 区域ID
     * @return 活跃预警列表
     */
    @GetMapping("/active/area")
    public Result<List<WarningEventDTO>> listActiveWarningsByArea(@RequestParam("areaId") Long areaId) {
        return Result.success(warningEventService.listActiveByArea(areaId));
    }

    /**
     * 查询预警事件详情（契约 getWarningById）。
     *
     * @param warningId 预警事件ID
     * @return 预警事件
     */
    @GetMapping("/{warningId:[0-9]+}")
    public Result<WarningEventDTO> getWarningById(@PathVariable("warningId") Long warningId) {
        return Result.success(warningEventService.getById(warningId));
    }

    /**
     * 创建预警事件（契约 createWarning，走统一幂等落库逻辑）。
     *
     * @param warningEvent 预警事件
     * @return 创建后的预警事件（含ID）
     */
    @PostMapping
    public Result<WarningEventDTO> createWarning(@RequestBody WarningEventDTO warningEvent) {
        return Result.success(warningEventService.create(warningEvent));
    }

    /**
     * 确认预警（状态机：0 待确认 → 1 已确认）。
     *
     * @param id 预警事件ID
     * @return 更新后的预警事件
     */
    @PostMapping("/{id:[0-9]+}/confirm")
    public Result<WarningEventDTO> confirm(@PathVariable("id") Long id) {
        return Result.success(warningEventService.confirm(id));
    }

    /**
     * 核销预警（状态机：未核销状态 → 3 已核销，写 resolved_at）。
     *
     * @param id 预警事件ID
     * @return 更新后的预警事件
     */
    @PostMapping("/{id:[0-9]+}/cancel")
    public Result<WarningEventDTO> cancel(@PathVariable("id") Long id) {
        return Result.success(warningEventService.cancel(id));
    }
}
