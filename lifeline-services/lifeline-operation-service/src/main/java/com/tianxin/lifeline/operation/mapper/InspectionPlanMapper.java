package com.tianxin.lifeline.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.operation.entity.InspectionPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 巡检计划 Mapper。
 *
 * <p>基础 CRUD 与分页由 MyBatis-Plus BaseMapper 与分页插件提供。
 */
@Mapper
public interface InspectionPlanMapper extends BaseMapper<InspectionPlan> {
}
