package com.tianxin.lifeline.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.asset.entity.PipeTopology;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管网拓扑关系 Mapper（见 PipeTopologyMapper.xml）。
 */
@Mapper
public interface PipeTopologyMapper extends BaseMapper<PipeTopology> {

    /**
     * 查询与指定管网相关的全部拓扑关系（作为起点或终点）。
     *
     * @param pipeId 管网ID
     * @return 拓扑关系列表
     */
    List<PipeTopology> selectByPipeId(@Param("pipeId") Long pipeId);
}
