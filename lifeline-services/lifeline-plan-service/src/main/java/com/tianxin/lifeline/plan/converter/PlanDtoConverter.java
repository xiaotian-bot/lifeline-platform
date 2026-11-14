package com.tianxin.lifeline.plan.converter;

import com.tianxin.lifeline.plan.constant.PlanInstanceStatus;
import com.tianxin.lifeline.plan.constant.PlanTaskStatus;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.dto.PlanLogVO;
import com.tianxin.lifeline.plan.dto.PlanResourceVO;
import com.tianxin.lifeline.plan.dto.PlanTaskVO;
import com.tianxin.lifeline.plan.entity.PlanInstance;
import com.tianxin.lifeline.plan.entity.PlanLog;
import com.tianxin.lifeline.plan.entity.PlanResource;
import com.tianxin.lifeline.plan.entity.PlanTask;

/**
 * 预案域实体与视图对象的集中转换器。
 *
 * <p>字段映射约定：
 * <ul>
 *     <li>实例/任务状态码统一附带中文文本（statusText）；</li>
 *     <li>模板名称/场景类型由 XML 多表关联回填，不经本转换器；</li>
 *     <li>入参为 null 时统一返回 null，避免调用方判空。</li>
 * </ul>
 */
public final class PlanDtoConverter {

    /**
     * 私有构造，工具类禁止实例化。
     */
    private PlanDtoConverter() {
    }

    /**
     * 预案实例实体转视图对象（不含模板关联字段与任务进度）。
     *
     * @param instance 实例实体
     * @return 实例视图对象，入参为 null 返回 null
     */
    public static PlanInstanceVO toInstanceVO(PlanInstance instance) {
        if (instance == null) {
            return null;
        }
        PlanInstanceVO vo = new PlanInstanceVO();
        vo.setId(instance.getId());
        vo.setInstanceCode(instance.getInstanceCode());
        vo.setPlanName(instance.getPlanName());
        vo.setTemplateId(instance.getTemplateId());
        vo.setWarningEventId(instance.getWarningEventId());
        vo.setStatus(instance.getStatus());
        vo.setStatusText(PlanInstanceStatus.toText(instance.getStatus()));
        vo.setSource(instance.getSource());
        vo.setGeneratedBy(instance.getGeneratedBy());
        vo.setSuggestion(instance.getSuggestion());
        vo.setEscalationLevel(instance.getEscalationLevel());
        vo.setStartTime(instance.getStartTime());
        vo.setEndTime(instance.getEndTime());
        vo.setCreatedAt(instance.getCreatedAt());
        return vo;
    }

    /**
     * 补齐实例视图对象的状态中文文本（XML 关联查询结果回填用）。
     *
     * @param vo 实例视图对象
     */
    public static void fillStatusText(PlanInstanceVO vo) {
        if (vo != null) {
            vo.setStatusText(PlanInstanceStatus.toText(vo.getStatus()));
        }
    }

    /**
     * 预案任务实体转视图对象。
     *
     * @param task 任务实体
     * @return 任务视图对象，入参为 null 返回 null
     */
    public static PlanTaskVO toTaskVO(PlanTask task) {
        if (task == null) {
            return null;
        }
        PlanTaskVO vo = new PlanTaskVO();
        vo.setId(task.getId());
        vo.setInstanceId(task.getInstanceId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskType(task.getTaskType());
        vo.setAssignee(task.getAssignee());
        vo.setStatus(task.getStatus());
        vo.setStatusText(PlanTaskStatus.toText(task.getStatus()));
        vo.setPriority(task.getPriority());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    /**
     * 预案资源实体转视图对象。
     *
     * @param resource 资源实体
     * @return 资源视图对象，入参为 null 返回 null
     */
    public static PlanResourceVO toResourceVO(PlanResource resource) {
        if (resource == null) {
            return null;
        }
        PlanResourceVO vo = new PlanResourceVO();
        vo.setId(resource.getId());
        vo.setInstanceId(resource.getInstanceId());
        vo.setResourceType(resource.getResourceType());
        vo.setResourceName(resource.getResourceName());
        vo.setQuantity(resource.getQuantity());
        vo.setLocation(resource.getLocation());
        vo.setStatus(resource.getStatus());
        return vo;
    }

    /**
     * 预案执行日志实体转视图对象。
     *
     * @param planLog 日志实体
     * @return 日志视图对象，入参为 null 返回 null
     */
    public static PlanLogVO toLogVO(PlanLog planLog) {
        if (planLog == null) {
            return null;
        }
        PlanLogVO vo = new PlanLogVO();
        vo.setId(planLog.getId());
        vo.setInstanceId(planLog.getInstanceId());
        vo.setAction(planLog.getAction());
        vo.setOperator(planLog.getOperator());
        vo.setContent(planLog.getContent());
        vo.setLogTime(planLog.getLogTime());
        return vo;
    }
}
