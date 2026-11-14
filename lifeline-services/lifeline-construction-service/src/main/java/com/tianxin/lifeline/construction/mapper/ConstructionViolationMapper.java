package com.tianxin.lifeline.construction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 施工违规记录 Mapper。
 *
 * <p>单表操作直接复用 MyBatis-Plus BaseMapper（含逻辑删除与自动填充）。
 */
@Mapper
public interface ConstructionViolationMapper extends BaseMapper<ConstructionViolation> {
}
