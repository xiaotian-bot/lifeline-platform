package com.tianxin.lifeline.construction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.construction.dto.NearbyPipeVO;
import com.tianxin.lifeline.construction.dto.ProjectVO;
import com.tianxin.lifeline.construction.entity.ConstructionProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 施工项目 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供（含逻辑删除与自动填充）；
 * 涉及 PostGIS geometry 列的分页/详情/插入/更新与周边管线空间查询
 * 定义在 mapper/ConstructionProjectMapper.xml 中（复用 pipe-asset
 * 空间模式：ST_AsText 读 / ST_GeomFromText 写 / GiST + {@code &&} 边界框预过滤）。
 */
@Mapper
public interface ConstructionProjectMapper extends BaseMapper<ConstructionProject> {

    /**
     * 分页查询施工项目（状态/区域关键字过滤，创建时间降序）。
     *
     * @param page   分页对象
     * @param status 项目状态（可空）
     * @param area   区域/关键字（可空，匹配项目名称/施工地点/施工单位）
     * @return 分页结果
     */
    Page<ConstructionProject> selectProjectPage(Page<ConstructionProject> page,
                                                @Param("status") Integer status,
                                                @Param("area") String area);

    /**
     * 查询施工项目详情（含 WKT 与经纬度坐标分解）。
     *
     * @param id 项目ID
     * @return 项目视图对象，不存在返回 null
     */
    ProjectVO selectDetailById(@Param("id") Long id);

    /**
     * 新增施工项目（geom 经 ST_GeomFromText 写入，EPSG:4326）。
     *
     * @param project 项目实体（含 id/createdAt/updatedAt/geomWkt）
     * @return 影响行数
     */
    int insertProject(ConstructionProject project);

    /**
     * 更新施工项目（仅更新非空字段；geomWkt 非空时重写几何）。
     *
     * @param project 项目实体（含 id 与 updatedAt）
     * @return 影响行数
     */
    int updateProject(ConstructionProject project);

    /**
     * 周边管线影响范围查询：以项目点位为圆心、半径内检索 pipe_main，
     * GiST 边界框（{@code &&}）预过滤 + ST_DWithin geography 精确判定，
     * 按球面距离升序返回。
     *
     * @param projectId    施工项目ID
     * @param radiusMeters 影响半径（米）
     * @param limit        最大返回条数
     * @return 周边管线列表（含距离，米）
     */
    List<NearbyPipeVO> selectNearbyPipes(@Param("projectId") Long projectId,
                                         @Param("radiusMeters") double radiusMeters,
                                         @Param("limit") int limit);
}
