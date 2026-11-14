package com.tianxin.lifeline.operation.service;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.operation.dto.InspectionPlanQueryRequest;
import com.tianxin.lifeline.operation.dto.InspectionPlanRequest;
import com.tianxin.lifeline.operation.dto.OrderVO;
import com.tianxin.lifeline.operation.entity.InspectionPlan;

import java.util.List;

/**
 * 巡检计划业务服务接口。
 *
 * <p>负责巡检计划分页查询、新增（新增时自动派生一张 INSPECT 巡检工单）
 * 与计划派生巡检工单查询。
 */
public interface InspectionPlanService {

    /**
     * 分页查询巡检计划。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<InspectionPlan> page(InspectionPlanQueryRequest request);

    /**
     * 新增巡检计划，并自动派生一张 INSPECT 巡检工单（source=PLAN）。
     *
     * @param request 计划新增参数
     * @return 新增的计划（含ID）
     */
    InspectionPlan create(InspectionPlanRequest request);

    /**
     * 查询巡检计划详情。
     *
     * @param id 计划ID
     * @return 计划详情，不存在抛业务异常
     */
    InspectionPlan detail(Long id);

    /**
     * 查询指定计划派生的巡检工单（order_type=INSPECT）。
     *
     * @param planId 计划ID
     * @return 派生的巡检工单列表
     */
    List<OrderVO> listTasks(Long planId);
}
