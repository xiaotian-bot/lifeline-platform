package com.tianxin.lifeline.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.asset.entity.Manhole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 井室 Mapper。
 *
 * <p>经纬度经 {@code ST_X/ST_Y} 提取（见 ManholeMapper.xml）。
 */
@Mapper
public interface ManholeMapper extends BaseMapper<Manhole> {

    /**
     * 分页查询井室（含经纬度），支持按类型/状态/编码关键字筛选。
     *
     * <p>LIMIT/OFFSET 与 COUNT 总数由 MyBatis-Plus 分页拦截器自动织入。
     *
     * @param page        分页对象
     * @param manholeType 井室类型（可空）
     * @param status      状态（可空）
     * @param keyword     井室编码关键字（可空）
     * @return 分页结果
     */
    IPage<Manhole> selectManholePage(Page<Manhole> page,
                                     @Param("manholeType") String manholeType,
                                     @Param("status") Integer status,
                                     @Param("keyword") String keyword);

    /**
     * 查询与指定管网空间邻近的井室（一管一档聚合用）。
     *
     * <p>manhole 表无 pipe_id 外键，改用 PostGIS 空间邻近判定：
     * {@code ST_DWithin} 按 geography 计算真实球面距离（米）。
     *
     * @param pipeId 管网ID
     * @param radius 邻近半径（米）
     * @return 邻近井室列表（含经纬度）
     */
    List<Manhole> selectNearPipe(@Param("pipeId") Long pipeId, @Param("radius") Double radius);
}
