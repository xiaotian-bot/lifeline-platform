package com.tianxin.lifeline.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.asset.entity.PipeDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管网设备 Mapper。
 *
 * <p>坐标经 {@code ST_X/ST_Y} 提取，管网类型经 JOIN pipe_main 回填（见 PipeDeviceMapper.xml）。
 */
@Mapper
public interface PipeDeviceMapper extends BaseMapper<PipeDevice> {

    /**
     * 按设备ID查询设备详情（含经纬度与所属管网类型）。
     *
     * @param id 设备ID
     * @return 设备实体，不存在返回 null
     */
    PipeDevice selectDeviceById(@Param("id") Long id);

    /**
     * 查询某管网上挂载的全部设备（含经纬度与所属管网类型）。
     *
     * @param pipeId 管网ID
     * @return 设备列表
     */
    List<PipeDevice> selectByPipeId(@Param("pipeId") Long pipeId);

    /**
     * 分页查询设备（含经纬度与所属管网类型），支持按类型/管网/状态/关键字筛选。
     *
     * <p>LIMIT/OFFSET 与 COUNT 总数由 MyBatis-Plus 分页拦截器自动织入。
     *
     * @param page       分页对象
     * @param deviceType 设备类型（可空）
     * @param pipeId     所属管网ID（可空）
     * @param status     状态（可空）
     * @param keyword    设备编码/名称关键字（可空）
     * @return 分页结果
     */
    IPage<PipeDevice> selectDevicePage(Page<PipeDevice> page,
                                       @Param("deviceType") String deviceType,
                                       @Param("pipeId") Long pipeId,
                                       @Param("status") Integer status,
                                       @Param("keyword") String keyword);
}
