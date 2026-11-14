package com.tianxin.lifeline.construction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;
import org.apache.ibatis.annotations.Mapper;

/**
 * 施工工序 Mapper。
 *
 * <p>工序 CRUD 与质检记录回写均为单表操作，直接复用
 * MyBatis-Plus BaseMapper（含逻辑删除与自动填充）。
 */
@Mapper
public interface ConstructionProcessMapper extends BaseMapper<ConstructionProcess> {
}
