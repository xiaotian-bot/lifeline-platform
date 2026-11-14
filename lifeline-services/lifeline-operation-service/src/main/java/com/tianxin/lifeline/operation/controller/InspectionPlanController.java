package com.tianxin.lifeline.operation.controller;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.operation.dto.InspectionPlanQueryRequest;
import com.tianxin.lifeline.operation.dto.InspectionPlanRequest;
import com.tianxin.lifeline.operation.dto.OrderVO;
import com.tianxin.lifeline.operation.entity.InspectionPlan;
import com.tianxin.lifeline.operation.service.InspectionPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 巡检计划 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/inspections，Controller 只做参数校验与
 * Result 包装，业务逻辑全部下沉服务层。
 */
@RestController
@RequestMapping("/api/v1/inspections")
public class InspectionPlanController {

    /** 巡检计划业务服务 */
    private final InspectionPlanService planService;

    /**
     * 构造控制器。
     *
     * @param planService 巡检计划业务服务
     */
    public InspectionPlanController(InspectionPlanService planService) {
        this.planService = planService;
    }

    /**
     * 分页查询巡检计划。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<InspectionPlan>> page(@Valid InspectionPlanQueryRequest request) {
        return Result.success(planService.page(request));
    }

    /**
     * 新增巡检计划（自动派生一张 INSPECT 巡检工单）。
     *
     * @param request 计划新增参数
     * @return 新增的计划（含ID）
     */
    @PostMapping
    public Result<InspectionPlan> create(@Valid @RequestBody InspectionPlanRequest request) {
        return Result.success(planService.create(request));
    }

    /**
     * 查询巡检计划详情。
     *
     * @param id 计划ID
     * @return 计划详情
     */
    @GetMapping("/{id}")
    public Result<InspectionPlan> detail(@PathVariable("id") Long id) {
        return Result.success(planService.detail(id));
    }

    /**
     * 查询指定计划派生的巡检工单（order_type=INSPECT）。
     *
     * @param id 计划ID
     * @return 派生的巡检工单列表
     */
    @GetMapping("/{id}/tasks")
    public Result<List<OrderVO>> tasks(@PathVariable("id") Long id) {
        return Result.success(planService.listTasks(id));
    }
}
