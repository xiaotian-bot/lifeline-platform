package com.tianxin.lifeline.construction.service;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.construction.dto.NearbyPipeVO;
import com.tianxin.lifeline.construction.dto.ProjectCreateRequest;
import com.tianxin.lifeline.construction.dto.ProjectDetailVO;
import com.tianxin.lifeline.construction.dto.ProjectQueryRequest;
import com.tianxin.lifeline.construction.dto.ProjectVO;

import java.util.List;

/**
 * 施工项目业务服务。
 *
 * <p>项目分页/详情/创建/更新/删除、状态机推进（条件更新防竞态）、
 * 周边管线 PostGIS 空间影响分析。
 */
public interface ConstructionProjectService {

    /**
     * 分页查询施工项目（状态/区域关键字过滤）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<ProjectVO> page(ProjectQueryRequest request);

    /**
     * 查询施工项目详情（含坐标、工序、违规、影像聚合）。
     *
     * @param id 项目ID
     * @return 详情聚合视图
     */
    ProjectDetailVO detail(Long id);

    /**
     * 创建施工项目（雪花 ID 手工填充，初始状态待开工）。
     *
     * @param request 创建请求
     * @return 项目视图对象
     */
    ProjectVO create(ProjectCreateRequest request);

    /**
     * 更新施工项目（按非空字段选择性覆盖，坐标非空时重写几何）。
     *
     * @param id      项目ID
     * @param request 更新请求
     * @return 项目视图对象
     */
    ProjectVO update(Long id, ProjectCreateRequest request);

    /**
     * 逻辑删除施工项目。
     *
     * @param id 项目ID
     */
    void delete(Long id);

    /**
     * 推进项目状态（状态机校验 + 条件更新按影响行数判定）。
     *
     * @param id     项目ID
     * @param target 目标状态
     * @return 流转后的项目视图对象
     */
    ProjectVO advanceStatus(Long id, Integer target);

    /**
     * 周边管线影响范围查询（施工点位为圆心、半径内管线按距离升序）。
     *
     * @param id           项目ID
     * @param radiusMeters 影响半径（米）
     * @param limit        最大返回条数
     * @return 周边管线列表
     */
    List<NearbyPipeVO> nearbyPipes(Long id, double radiusMeters, int limit);
}
