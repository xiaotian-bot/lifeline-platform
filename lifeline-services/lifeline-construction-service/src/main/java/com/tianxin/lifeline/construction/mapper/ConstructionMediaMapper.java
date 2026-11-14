package com.tianxin.lifeline.construction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import org.apache.ibatis.annotations.Mapper;

/**
 * 施工影像 Mapper。
 *
 * <p>单表操作直接复用 MyBatis-Plus BaseMapper（含逻辑删除与自动填充），
 * 影像二进制存 MinIO，本表只存对象键。
 */
@Mapper
public interface ConstructionMediaMapper extends BaseMapper<ConstructionMedia> {
}
