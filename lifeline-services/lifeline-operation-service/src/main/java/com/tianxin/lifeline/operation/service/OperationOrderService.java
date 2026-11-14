package com.tianxin.lifeline.operation.service;

import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.operation.dto.OrderQueryRequest;
import com.tianxin.lifeline.operation.dto.OrderStatisticsVO;
import com.tianxin.lifeline.operation.dto.OrderVO;

import java.util.List;

/**
 * 运维工单业务服务接口。
 *
 * <p>封装工单创建、状态机流转（接单/完成/关闭）、分页查询、统计与
 * 工单事件/处置反馈事件的 Kafka 发布。
 */
public interface OperationOrderService {

    /**
     * 创建运维工单（与 OperationFeignClient 契约对齐）。
     *
     * <p>指派责任人则状态取 1 待接单，否则取 0 待派单；
     * 创建成功后发布 ORDER_CREATED 事件至 pipe_order_event。
     *
     * @param dto 工单创建参数
     * @return 创建成功的工单ID
     */
    Long createOrder(OrderCreateDTO dto);

    /**
     * 分页查询工单（红色预警应急工单默认置顶）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<OrderVO> page(OrderQueryRequest request);

    /**
     * 查询工单详情。
     *
     * @param id 工单ID
     * @return 工单详情，不存在抛业务异常
     */
    OrderVO detail(Long id);

    /**
     * 接单（状态机：1 待接单 → 2 处置中，写 start_time）。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    OrderVO accept(Long id);

    /**
     * 完成（状态机：2 处置中 → 3 已完成，写 result / end_time）。
     *
     * <p>同时发布 ORDER_COMPLETED 工单事件与 DISPOSAL_FEEDBACK 处置反馈事件。
     *
     * @param id     工单ID
     * @param result 处置结果描述
     * @return 流转后的工单
     */
    OrderVO complete(Long id, String result);

    /**
     * 关闭（状态机：0/1/2 → 4 已关闭，写 end_time）。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    OrderVO close(Long id);

    /**
     * 按状态与工单类型分组统计。
     *
     * @return 统计结果
     */
    OrderStatisticsVO statistics();

    /**
     * 查询指定预警事件关联的工单列表。
     *
     * @param warningEventId 预警事件ID
     * @return 关联工单列表
     */
    List<OrderVO> listByWarningEventId(Long warningEventId);

    /**
     * 查询指定巡检计划派生的巡检工单（order_type=INSPECT）。
     *
     * @param planId 巡检计划ID
     * @return 派生的巡检工单列表
     */
    List<OrderVO> listInspectionTasks(Long planId);
}
