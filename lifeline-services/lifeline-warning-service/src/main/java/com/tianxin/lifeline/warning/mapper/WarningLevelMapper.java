package com.tianxin.lifeline.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.warning.entity.WarningLevelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预警等级配置 Mapper。
 *
 * <p>基础查询由 MyBatis-Plus BaseMapper 提供，用于按等级匹配
 * push_scope（推送对象）与 response_require（响应要求）。
 */
@Mapper
public interface WarningLevelMapper extends BaseMapper<WarningLevelEntity> {
}
