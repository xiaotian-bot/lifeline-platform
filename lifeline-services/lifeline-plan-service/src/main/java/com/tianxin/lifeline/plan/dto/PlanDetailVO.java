package com.tianxin.lifeline.plan.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预案实例详情聚合视图对象。
 *
 * <p>一次返回实例主体（含模板关联与任务进度）、任务清单、
 * 资源清单与执行日志，供前端预案详情页渲染。
 */
@Data
public class PlanDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例主体信息（含模板名称/场景类型/任务进度聚合） */
    private PlanInstanceVO instance;

    /** 任务清单（优先级升序、创建时间升序） */
    private List<PlanTaskVO> tasks;

    /** 资源清单 */
    private List<PlanResourceVO> resources;

    /** 执行日志（时间倒序） */
    private List<PlanLogVO> logs;
}
