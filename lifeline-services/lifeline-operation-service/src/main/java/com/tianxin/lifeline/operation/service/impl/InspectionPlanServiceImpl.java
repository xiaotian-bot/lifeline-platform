package com.tianxin.lifeline.operation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.operation.constant.OrderSource;
import com.tianxin.lifeline.operation.constant.OrderType;
import com.tianxin.lifeline.operation.dto.InspectionPlanQueryRequest;
import com.tianxin.lifeline.operation.dto.InspectionPlanRequest;
import com.tianxin.lifeline.operation.dto.OrderVO;
import com.tianxin.lifeline.operation.entity.InspectionPlan;
import com.tianxin.lifeline.operation.mapper.InspectionPlanMapper;
import com.tianxin.lifeline.operation.service.InspectionPlanService;
import com.tianxin.lifeline.operation.service.OperationOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 巡检计划业务服务实现。
 *
 * <p>新增计划时自动派生一张 INSPECT 巡检工单：source=PLAN，
 * content 以 [inspectionPlanId=计划ID] 标记关联，供 listTasks 反查。
 */
@Slf4j
@Service
public class InspectionPlanServiceImpl implements InspectionPlanService {

    /** 巡检计划 Mapper */
    private final InspectionPlanMapper planMapper;

    /** 工单业务服务（派生巡检工单使用） */
    private final OperationOrderService orderService;

    /**
     * 构造服务（构造器注入，工单服务不反向依赖本服务，无循环依赖）。
     *
     * @param planMapper   巡检计划 Mapper
     * @param orderService 工单业务服务
     */
    public InspectionPlanServiceImpl(InspectionPlanMapper planMapper, OperationOrderService orderService) {
        this.planMapper = planMapper;
        this.orderService = orderService;
    }

    /**
     * 分页查询巡检计划（名称模糊 / 类型 / 区域 / 状态过滤，创建时间降序）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<InspectionPlan> page(InspectionPlanQueryRequest request) {
        LambdaQueryWrapper<InspectionPlan> wrapper = new LambdaQueryWrapper<InspectionPlan>()
                .like(StringUtils.hasText(request.getPlanName()), InspectionPlan::getPlanName, request.getPlanName())
                .eq(StringUtils.hasText(request.getPlanType()), InspectionPlan::getPlanType, request.getPlanType())
                .eq(StringUtils.hasText(request.getAreaId()), InspectionPlan::getAreaId, request.getAreaId())
                .eq(request.getStatus() != null, InspectionPlan::getStatus, request.getStatus())
                .orderByDesc(InspectionPlan::getCreatedAt);
        Page<InspectionPlan> page = planMapper.selectPage(
                new Page<>(request.getPage(), request.getSize()), wrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), request.getPage(), request.getSize());
    }

    /**
     * 新增巡检计划并派生巡检工单。
     *
     * @param request 计划新增参数
     * @return 新增的计划（含ID）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionPlan create(InspectionPlanRequest request) {
        InspectionPlan plan = new InspectionPlan();
        plan.setPlanName(request.getPlanName());
        plan.setPlanType(request.getPlanType());
        plan.setAreaId(request.getAreaId());
        plan.setFrequency(request.getFrequency());
        plan.setStatus(1);
        planMapper.insert(plan);

        deriveInspectionOrder(plan);
        log.info("新增巡检计划成功并派生巡检工单: planId={}, planName={}", plan.getId(), plan.getPlanName());
        return plan;
    }

    /**
     * 查询巡检计划详情。
     *
     * @param id 计划ID
     * @return 计划详情
     */
    @Override
    public InspectionPlan detail(Long id) {
        InspectionPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new BizException(ErrorCode.INSPECTION_NOT_FOUND);
        }
        return plan;
    }

    /**
     * 查询指定计划派生的巡检工单。
     *
     * @param planId 计划ID
     * @return 派生的巡检工单列表
     */
    @Override
    public List<OrderVO> listTasks(Long planId) {
        // 校验计划存在
        detail(planId);
        return orderService.listInspectionTasks(planId);
    }

    /**
     * 为计划派生一张 INSPECT 巡检工单（content 携带计划ID关联标记）。
     *
     * @param plan 巡检计划
     */
    private void deriveInspectionOrder(InspectionPlan plan) {
        OrderCreateDTO dto = new OrderCreateDTO();
        dto.setTitle(plan.getPlanName() + "-巡检工单");
        dto.setOrderType(OrderType.INSPECT);
        dto.setPriority(2);
        dto.setSource(OrderSource.PLAN);
        dto.setDescription("[inspectionPlanId=" + plan.getId() + "] 由巡检计划【"
                + plan.getPlanName() + "】派生，巡检频次："
                + (plan.getFrequency() == null ? "未设置" : plan.getFrequency()));
        orderService.createOrder(dto);
    }
}
