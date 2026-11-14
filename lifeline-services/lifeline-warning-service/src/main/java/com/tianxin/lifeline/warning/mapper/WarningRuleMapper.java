package com.tianxin.lifeline.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.warning.entity.WarningRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预警规则 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供（含逻辑删除与自动填充）。
 */
@Mapper
public interface WarningRuleMapper extends BaseMapper<WarningRule> {
}
