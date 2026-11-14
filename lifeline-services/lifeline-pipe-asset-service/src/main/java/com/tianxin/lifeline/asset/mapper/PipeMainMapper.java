package com.tianxin.lifeline.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.asset.entity.PipeMain;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管网主线 Mapper。
 *
 * <p>geom 字段通过 PostGIS 空间函数读写（见 PipeMainMapper.xml）。
 */
@Mapper
public interface PipeMainMapper extends BaseMapper<PipeMain> {

    /**
     * 分页查询管网（含 WKT），支持按类型/区域关键字/状态筛选。
     *
     * @param page     分页对象
     * @param pipeType 管网类型（可空）
     * @param area     区域/名称关键字（可空）
     * @param status   状态（可空）
     * @return 分页结果
     */
    IPage<PipeMain> selectPipePage(Page<PipeMain> page,
                                   @Param("pipeType") String pipeType,
                                   @Param("area") String area,
                                   @Param("status") Integer status);

    /**
     * 查询管网详情（含 WKT）。
     *
     * @param id 管网ID
     * @return 管网实体
     */
    PipeMain selectDetailById(@Param("id") Long id);

    /**
     * 新增管网（含空间数据）。
     *
     * @param pipe 管网实体（geomWkt 为 WKT 文本）
     * @return 影响行数
     */
    int insertPipe(PipeMain pipe);

    /**
     * 更新管网（含空间数据）。
     *
     * @param pipe 管网实体
     * @return 影响行数
     */
    int updatePipe(PipeMain pipe);

    /**
     * 查询全部管网的空间数据（GeoJSON），用于 FeatureCollection 输出。
     *
     * @param pipeType 管网类型（可空）
     * @return 管网列表（geomGeoJson 已填充）
     */
    List<PipeMain> selectGeoJsonList(@Param("pipeType") String pipeType);

    /**
     * 按管网类型查询列表（供 Feign 调用）。
     *
     * @param pipeType 管网类型
     * @return 管网列表
     */
    List<PipeMain> selectByType(@Param("pipeType") String pipeType);
}
