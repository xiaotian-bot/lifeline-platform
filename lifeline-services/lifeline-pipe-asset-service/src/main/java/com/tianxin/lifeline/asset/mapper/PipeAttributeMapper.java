package com.tianxin.lifeline.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.asset.entity.PipeAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管网扩展属性 Mapper（KV 结构，见 PipeAttributeMapper.xml）。
 */
@Mapper
public interface PipeAttributeMapper extends BaseMapper<PipeAttribute> {

    /**
     * 按管网ID查询扩展属性列表。
     *
     * @param pipeId 管网ID
     * @return 扩展属性列表
     */
    List<PipeAttribute> selectByPipeId(@Param("pipeId") Long pipeId);

    /**
     * 按管网ID集合批量查询扩展属性。
     *
     * @param pipeIds 管网ID集合
     * @return 扩展属性列表
     */
    List<PipeAttribute> selectByPipeIds(@Param("pipeIds") List<Long> pipeIds);

    /**
     * 批量插入扩展属性（id/created_at/updated_at 由应用层预填充）。
     *
     * @param list 扩展属性列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<PipeAttribute> list);

    /**
     * 按管网ID逻辑删除全部扩展属性（用于更新时全量替换）。
     *
     * @param pipeId 管网ID
     * @return 影响行数
     */
    int logicDeleteByPipeId(@Param("pipeId") Long pipeId);
}
