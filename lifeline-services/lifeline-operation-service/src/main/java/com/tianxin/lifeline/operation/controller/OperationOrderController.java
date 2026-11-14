package com.tianxin.lifeline.operation.controller;

import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.operation.dto.OrderCompleteRequest;
import com.tianxin.lifeline.operation.dto.OrderQueryRequest;
import com.tianxin.lifeline.operation.dto.OrderStatisticsVO;
import com.tianxin.lifeline.operation.dto.OrderVO;
import com.tianxin.lifeline.operation.service.OperationOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维工单 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/orders，与 OperationFeignClient 契约对齐
 * （POST 创建签名 OrderCreateDTO → Result&lt;Long&gt; 完全一致），
 * Controller 只做参数校验与 Result 包装，业务逻辑全部下沉服务层。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OperationOrderController {

    /** 工单业务服务 */
    private final OperationOrderService orderService;

    /**
     * 构造控制器。
     *
     * @param orderService 工单业务服务
     */
    public OperationOrderController(OperationOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 分页查询工单（支持状态/类型/预警事件/责任人/优先级/时间范围过滤，
     * 红色预警应急工单默认置顶）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<OrderVO>> page(@Valid OrderQueryRequest request) {
        return Result.success(orderService.page(request));
    }

    /**
     * 按状态与工单类型分组统计。
     *
     * @return 统计结果
     */
    @GetMapping("/statistics")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderService.statistics());
    }

    /**
     * 查询工单详情。
     *
     * @param id 工单ID
     * @return 工单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(orderService.detail(id));
    }

    /**
     * 创建运维工单（契约 createOrder：OrderCreateDTO → Result&lt;Long&gt;）。
     *
     * <p>指派责任人则状态取 1 待接单，否则 0 待派单。
     *
     * @param orderCreateDTO 工单创建参数
     * @return 创建成功的工单ID
     */
    @PostMapping
    public Result<Long> createOrder(@RequestBody OrderCreateDTO orderCreateDTO) {
        return Result.success(orderService.createOrder(orderCreateDTO));
    }

    /**
     * 接单（状态机：1 待接单 → 2 处置中）。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    @PostMapping("/{id}/accept")
    public Result<OrderVO> accept(@PathVariable("id") Long id) {
        return Result.success(orderService.accept(id));
    }

    /**
     * 完成（状态机：2 处置中 → 3 已完成，请求体携带处置结果）。
     *
     * @param id      工单ID
     * @param request 完成请求（result 必填）
     * @return 流转后的工单
     */
    @PostMapping("/{id}/complete")
    public Result<OrderVO> complete(@PathVariable("id") Long id,
                                    @Valid @RequestBody OrderCompleteRequest request) {
        return Result.success(orderService.complete(id, request.getResult()));
    }

    /**
     * 关闭（状态机：0/1/2 → 4 已关闭）。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    @PostMapping("/{id}/close")
    public Result<OrderVO> close(@PathVariable("id") Long id) {
        return Result.success(orderService.close(id));
    }
}
