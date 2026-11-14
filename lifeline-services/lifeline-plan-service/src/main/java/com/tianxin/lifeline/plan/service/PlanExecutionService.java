package com.tianxin.lifeline.plan.service;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.plan.dto.DisposalFeedbackPayload;
import com.tianxin.lifeline.plan.dto.PlanDetailVO;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.dto.PlanTaskUpdateRequest;
import com.tianxin.lifeline.plan.dto.PlanTaskVO;

import java.util.List;

/**
 * 预案执行业务服务。
 *
 * <p>状态机：实例 0 草稿 → 1 执行中 → 2 已完成，0/1 可取消（→ 4）；
 * 任务状态推进至全部完成后实例自动核销；每步写 plan_log 留痕；
 * 启动执行时可选经工单服务派单（失败降级仅记日志不阻断）；
 * 消费处置反馈事件按 warningEventId 关联回写任务状态。
 */
public interface PlanExecutionService {

    /**
     * 查询预案实例详情（实例 + 任务 + 资源 + 日志聚合）。
     *
     * @param instanceId 实例ID
     * @return 详情聚合视图
     */
    PlanDetailVO detail(Long instanceId);

    /**
     * 启动执行预案（状态机：0 草稿 → 1 执行中）。
     *
     * @param instanceId    实例ID
     * @param dispatchOrder 是否联动工单服务派单（失败降级不阻断）
     * @return 流转后的实例视图
     */
    PlanInstanceVO execute(Long instanceId, boolean dispatchOrder);

    /**
     * 查询实例下的任务清单。
     *
     * @param instanceId 实例ID
     * @return 任务列表（优先级升序、创建时间升序）
     */
    List<PlanTaskVO> listTasks(Long instanceId);

    /**
     * 推进任务状态（全部任务完成后实例自动核销）。
     *
     * @param taskId  任务ID
     * @param request 状态推进请求
     * @return 流转后的任务视图
     */
    PlanTaskVO updateTask(Long taskId, PlanTaskUpdateRequest request);

    /**
     * 处置反馈回写：按预警事件ID关联执行中实例，
     * 将未完成任务置为已完成并核销实例。
     *
     * @param payload 处置反馈负载
     */
    void applyDisposalFeedback(DisposalFeedbackPayload payload);

    /**
     * 三级联动上报：仅执行中实例可上报，级别逐级 +1 至上限 2
     * （0 企业运维 / 1 主管部门 / 2 应急联动），写 plan_log 留痕。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    PlanInstanceVO escalate(Long instanceId);

    /**
     * 取消预案（草稿/执行中可取消 → 4 已取消）。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    PlanInstanceVO cancel(Long instanceId);

    /**
     * 归档预案（已完成可归档 → 3 已归档）。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    PlanInstanceVO archive(Long instanceId);

    /**
     * 分页查询预案实例列表。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<PlanInstanceVO> listInstances(int page, int size);
}
