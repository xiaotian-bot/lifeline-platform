package com.tianxin.lifeline.plan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.entity.PlanInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 预案执行实例 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供（含逻辑删除与自动填充），
 * 实例详情多表关联 / 任务进度聚合统计 / 按预警事件查询定义在
 * mapper/PlanInstanceMapper.xml 中。
 */
@Mapper
public interface PlanInstanceMapper extends BaseMapper<PlanInstance> {

    /**
     * 查询实例详情（关联 plan_template 回填模板名称与场景类型）。
     *
     * @param id 实例ID
     * @return 实例详情视图对象，不存在返回 null
     */
    PlanInstanceVO selectInstanceDetail(@Param("id") Long id);

    /**
     * 按任务状态分组聚合统计指定实例的任务进度。
     *
     * @param instanceId 实例ID
     * @return 分组结果列表，每项含 group_key（任务状态数值）与 cnt（数量）
     */
    List<Map<String, Object>> countTaskGroupByStatus(@Param("instanceId") Long instanceId);

    /**
     * 按预警事件ID与实例状态查询实例列表（供处置反馈回写定位执行中实例）。
     *
     * @param warningEventId 预警事件ID
     * @param status         实例状态（可空，空则不过滤）
     * @return 实例列表（按创建时间降序）
     */
    List<PlanInstance> selectByWarningEventId(@Param("warningEventId") Long warningEventId,
                                              @Param("status") Integer status);
}
