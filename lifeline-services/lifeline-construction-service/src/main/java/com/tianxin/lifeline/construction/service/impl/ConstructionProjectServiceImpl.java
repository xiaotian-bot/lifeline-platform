package com.tianxin.lifeline.construction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.construction.constant.ProjectStatus;
import com.tianxin.lifeline.construction.converter.ConstructionDtoConverter;
import com.tianxin.lifeline.construction.dto.NearbyPipeVO;
import com.tianxin.lifeline.construction.dto.ProjectCreateRequest;
import com.tianxin.lifeline.construction.dto.ProjectDetailVO;
import com.tianxin.lifeline.construction.dto.ProjectQueryRequest;
import com.tianxin.lifeline.construction.dto.ProjectVO;
import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;
import com.tianxin.lifeline.construction.entity.ConstructionProject;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import com.tianxin.lifeline.construction.mapper.ConstructionMediaMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionProcessMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionProjectMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionViolationMapper;
import com.tianxin.lifeline.construction.service.ConstructionProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 施工项目业务服务实现。
 *
 * <p>项目状态机：0 待开工 → 1 施工中 → 2 已完工；施工中可停工 → 3，
 * 已停工可复工 → 1；状态推进采用条件更新按影响行数判定，规避并发竞态。
 * geom 几何读写走 XML 空间 SQL（ST_GeomFromText / ST_AsText / ST_X/ST_Y）。
 */
@Slf4j
@Service
public class ConstructionProjectServiceImpl implements ConstructionProjectService {

    /** 项目 Mapper */
    private final ConstructionProjectMapper projectMapper;

    /** 工序 Mapper */
    private final ConstructionProcessMapper processMapper;

    /** 违规 Mapper */
    private final ConstructionViolationMapper violationMapper;

    /** 影像 Mapper */
    private final ConstructionMediaMapper mediaMapper;

    /**
     * 构造服务（构造器注入）。
     *
     * @param projectMapper   项目 Mapper
     * @param processMapper   工序 Mapper
     * @param violationMapper 违规 Mapper
     * @param mediaMapper     影像 Mapper
     */
    public ConstructionProjectServiceImpl(ConstructionProjectMapper projectMapper,
                                          ConstructionProcessMapper processMapper,
                                          ConstructionViolationMapper violationMapper,
                                          ConstructionMediaMapper mediaMapper) {
        this.projectMapper = projectMapper;
        this.processMapper = processMapper;
        this.violationMapper = violationMapper;
        this.mediaMapper = mediaMapper;
    }

    /**
     * 分页查询施工项目（状态/区域关键字过滤，创建时间降序）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<ProjectVO> page(ProjectQueryRequest request) {
        Page<ConstructionProject> page = projectMapper.selectProjectPage(
                new Page<>(request.getPage(), request.getSize()),
                request.getStatus(), request.getArea());
        PageResult<ConstructionProject> raw = PageResult.of(
                page.getRecords(), page.getTotal(), request.getPage(), request.getSize());
        return raw.map(ConstructionDtoConverter::toProjectVO);
    }

    /**
     * 查询施工项目详情（项目 + 工序 + 违规 + 影像聚合）。
     *
     * @param id 项目ID
     * @return 详情聚合视图
     */
    @Override
    public ProjectDetailVO detail(Long id) {
        ProjectVO project = projectMapper.selectDetailById(id);
        if (project == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND);
        }
        ConstructionDtoConverter.fillStatusText(project);

        ProjectDetailVO detail = new ProjectDetailVO();
        detail.setProject(project);

        LambdaQueryWrapper<ConstructionProcess> processWrapper = new LambdaQueryWrapper<>();
        processWrapper.eq(ConstructionProcess::getProjectId, id)
                .orderByAsc(ConstructionProcess::getCreatedAt).orderByAsc(ConstructionProcess::getId);
        detail.setProcesses(processMapper.selectList(processWrapper));

        LambdaQueryWrapper<ConstructionViolation> violationWrapper = new LambdaQueryWrapper<>();
        violationWrapper.eq(ConstructionViolation::getProjectId, id)
                .orderByDesc(ConstructionViolation::getCreatedAt);
        detail.setViolations(violationMapper.selectList(violationWrapper));

        LambdaQueryWrapper<ConstructionMedia> mediaWrapper = new LambdaQueryWrapper<>();
        mediaWrapper.eq(ConstructionMedia::getProjectId, id)
                .orderByDesc(ConstructionMedia::getCaptureTime);
        detail.setMedia(mediaMapper.selectList(mediaWrapper));
        return detail;
    }

    /**
     * 创建施工项目（雪花 ID 手工填充，初始状态待开工，坐标拼装 POINT WKT）。
     *
     * @param request 创建请求
     * @return 项目视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO create(ProjectCreateRequest request) {
        ConstructionProject project = new ConstructionProject();
        project.setId(SnowflakeIdGenerator.getInstance().nextId());
        project.setProjectName(request.getProjectName().trim());
        project.setLocation(request.getLocation());
        project.setContractor(request.getContractor());
        project.setSupervisor(request.getSupervisor());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setStatus(ProjectStatus.PENDING_START);
        project.setGeomWkt(buildPointWkt(request.getLongitude(), request.getLatitude()));
        LocalDateTime now = LocalDateTime.now();
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        projectMapper.insertProject(project);
        log.info("创建施工项目: id={}, name={}, status={}", project.getId(), project.getProjectName(),
                ProjectStatus.toText(project.getStatus()));
        return ConstructionDtoConverter.toProjectVO(project);
    }

    /**
     * 更新施工项目（按非空字段选择性覆盖，坐标齐全时重写几何）。
     *
     * @param id      项目ID
     * @param request 更新请求
     * @return 项目视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO update(Long id, ProjectCreateRequest request) {
        loadProject(id);
        ConstructionProject project = new ConstructionProject();
        project.setId(id);
        project.setProjectName(request.getProjectName().trim());
        project.setLocation(request.getLocation());
        project.setContractor(request.getContractor());
        project.setSupervisor(request.getSupervisor());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setGeomWkt(buildPointWkt(request.getLongitude(), request.getLatitude()));
        project.setUpdatedAt(LocalDateTime.now());
        if (projectMapper.updateProject(project) == 0) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND);
        }
        log.info("更新施工项目: id={}", id);
        return detailProjectVO(id);
    }

    /**
     * 逻辑删除施工项目。
     *
     * @param id 项目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        loadProject(id);
        projectMapper.deleteById(id);
        log.info("逻辑删除施工项目: id={}", id);
    }

    /**
     * 推进项目状态（状态机校验 + 条件更新按影响行数判定，防并发竞态）。
     *
     * @param id     项目ID
     * @param target 目标状态
     * @return 流转后的项目视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVO advanceStatus(Long id, Integer target) {
        ConstructionProject project = loadProject(id);
        Integer current = project.getStatus();
        if (target == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标状态不能为空");
        }
        if (!ProjectStatus.canTransit(current, target)) {
            throw new BizException(ErrorCode.INSPECT_PROCESS_ERROR,
                    "项目状态不允许从" + ProjectStatus.toText(current)
                            + "流转至" + ProjectStatus.toText(target));
        }
        // 条件更新防并发竞态：仅当前状态未变时可 CAS 流转，影响行数为 0 即并发失败
        LambdaUpdateWrapper<ConstructionProject> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ConstructionProject::getId, id)
                .eq(ConstructionProject::getStatus, current)
                .set(ConstructionProject::getStatus, target);
        if (projectMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.INSPECT_PROCESS_ERROR, "状态推进失败，项目状态已变化，请刷新后重试");
        }
        log.info("施工项目状态推进: id={}, {} -> {}", id,
                ProjectStatus.toText(current), ProjectStatus.toText(target));
        return detailProjectVO(id);
    }

    /**
     * 周边管线影响范围查询（GiST 边界框预过滤 + geography 半径精确判定）。
     *
     * @param id           项目ID
     * @param radiusMeters 影响半径（米）
     * @param limit        最大返回条数
     * @return 周边管线列表（距离升序）
     */
    @Override
    public List<NearbyPipeVO> nearbyPipes(Long id, double radiusMeters, int limit) {
        loadProject(id);
        return projectMapper.selectNearbyPipes(id, radiusMeters, limit);
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 加载项目，不存在抛业务异常。
     *
     * @param id 项目ID
     * @return 项目实体
     */
    private ConstructionProject loadProject(Long id) {
        ConstructionProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND);
        }
        return project;
    }

    /**
     * 查询详情视图（回填状态文本）。
     *
     * @param id 项目ID
     * @return 项目视图对象
     */
    private ProjectVO detailProjectVO(Long id) {
        ProjectVO vo = projectMapper.selectDetailById(id);
        ConstructionDtoConverter.fillStatusText(vo);
        return vo;
    }

    /**
     * 经纬度拼装 POINT WKT（任一为空返回 null，几何不写入）。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return POINT WKT 文本或 null
     */
    private String buildPointWkt(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            return null;
        }
        return "POINT(" + longitude + " " + latitude + ")";
    }
}
