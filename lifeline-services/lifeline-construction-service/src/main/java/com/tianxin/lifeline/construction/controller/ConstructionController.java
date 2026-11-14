package com.tianxin.lifeline.construction.controller;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.construction.dto.MediaUploadVO;
import com.tianxin.lifeline.construction.dto.NearbyPipeVO;
import com.tianxin.lifeline.construction.dto.ProcessCreateRequest;
import com.tianxin.lifeline.construction.dto.ProcessInspectRequest;
import com.tianxin.lifeline.construction.dto.ProjectCreateRequest;
import com.tianxin.lifeline.construction.dto.ProjectDetailVO;
import com.tianxin.lifeline.construction.dto.ProjectQueryRequest;
import com.tianxin.lifeline.construction.dto.ProjectVO;
import com.tianxin.lifeline.construction.dto.ViolationReportRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectResultVO;
import com.tianxin.lifeline.construction.entity.ConstructionMedia;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import com.tianxin.lifeline.construction.service.ConstructionMediaService;
import com.tianxin.lifeline.construction.service.ConstructionProcessService;
import com.tianxin.lifeline.construction.service.ConstructionProjectService;
import com.tianxin.lifeline.construction.service.ConstructionViolationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 施工监护 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/constructions，与网关路由
 * /api/v1/constructions/** → construction-service 对齐。
 * Controller 只做参数校验与 Result 包装，业务逻辑全部下沉服务层。
 */
@Validated
@RestController
@RequestMapping("/api/v1/constructions")
public class ConstructionController {

    /** 周边管线查询默认半径（米） */
    private static final double DEFAULT_NEARBY_RADIUS = 200.0;

    /** 周边管线查询默认条数上限 */
    private static final int DEFAULT_NEARBY_LIMIT = 50;

    /** 施工项目业务服务 */
    private final ConstructionProjectService projectService;

    /** 施工工序业务服务 */
    private final ConstructionProcessService processService;

    /** 施工违规业务服务 */
    private final ConstructionViolationService violationService;

    /** 施工影像业务服务 */
    private final ConstructionMediaService mediaService;

    /**
     * 构造控制器（构造器注入）。
     *
     * @param projectService   施工项目业务服务
     * @param processService   施工工序业务服务
     * @param violationService 施工违规业务服务
     * @param mediaService     施工影像业务服务
     */
    public ConstructionController(ConstructionProjectService projectService,
                                  ConstructionProcessService processService,
                                  ConstructionViolationService violationService,
                                  ConstructionMediaService mediaService) {
        this.projectService = projectService;
        this.processService = processService;
        this.violationService = violationService;
        this.mediaService = mediaService;
    }

    /* ==================== 施工项目 ==================== */

    /**
     * 分页查询施工项目（状态/区域关键字过滤）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @GetMapping("/projects")
    public Result<PageResult<ProjectVO>> pageProjects(@Valid ProjectQueryRequest request) {
        return Result.success(projectService.page(request));
    }

    /**
     * 创建施工项目（初始状态待开工，坐标写入 PostGIS 点位）。
     *
     * @param request 创建请求
     * @return 项目视图对象
     */
    @PostMapping("/projects")
    public Result<ProjectVO> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.create(request));
    }

    /**
     * 查询施工项目详情（项目 + 工序 + 违规 + 影像聚合）。
     *
     * @param id 项目ID
     * @return 详情聚合视图
     */
    @GetMapping("/projects/{id}")
    public Result<ProjectDetailVO> projectDetail(@PathVariable("id") Long id) {
        return Result.success(projectService.detail(id));
    }

    /**
     * 更新施工项目（按非空字段选择性覆盖）。
     *
     * @param id      项目ID
     * @param request 更新请求
     * @return 项目视图对象
     */
    @PutMapping("/projects/{id}")
    public Result<ProjectVO> updateProject(@PathVariable("id") Long id,
                                           @Valid @RequestBody ProjectCreateRequest request) {
        return Result.success(projectService.update(id, request));
    }

    /**
     * 逻辑删除施工项目。
     *
     * @param id 项目ID
     * @return 空结果
     */
    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.delete(id);
        return Result.success();
    }

    /**
     * 推进项目状态（状态机：0待开工→1施工中→2已完工；1⇄3停复工）。
     *
     * @param id     项目ID
     * @param target 目标状态
     * @return 流转后的项目视图对象
     */
    @PostMapping("/projects/{id}/status")
    public Result<ProjectVO> advanceStatus(@PathVariable("id") Long id,
                                           @RequestParam("target") Integer target) {
        return Result.success(projectService.advanceStatus(id, target));
    }

    /**
     * 周边管线影响范围查询（施工点位为圆心，GiST 边界框预过滤 +
     * geography 半径精确判定，按球面距离升序）。
     *
     * @param id           项目ID
     * @param radiusMeters 影响半径（米，1-5000，默认200）
     * @param limit        最大返回条数（1-200，默认50）
     * @return 周边管线列表
     */
    @GetMapping("/projects/{id}/nearby-pipes")
    public Result<List<NearbyPipeVO>> nearbyPipes(
            @PathVariable("id") Long id,
            @RequestParam(value = "radiusMeters", defaultValue = "200")
            @Min(value = 1, message = "影响半径不能小于1米")
            @Max(value = 5000, message = "影响半径不能超过5000米") double radiusMeters,
            @RequestParam(value = "limit", defaultValue = "50")
            @Min(value = 1, message = "条数上限不能小于1")
            @Max(value = 200, message = "条数上限不能超过200") int limit) {
        return Result.success(projectService.nearbyPipes(id,
                radiusMeters <= 0 ? DEFAULT_NEARBY_RADIUS : radiusMeters,
                limit <= 0 ? DEFAULT_NEARBY_LIMIT : limit));
    }

    /* ==================== 施工工序与质检 ==================== */

    /**
     * 查询项目下的工序清单（含质检记录）。
     *
     * @param projectId 项目ID
     * @return 工序列表
     */
    @GetMapping("/projects/{projectId}/processes")
    public Result<List<ConstructionProcess>> listProcesses(@PathVariable("projectId") Long projectId) {
        return Result.success(processService.listByProject(projectId));
    }

    /**
     * 创建工序（初始质检状态待质检）。
     *
     * @param projectId 项目ID
     * @param request   工序创建请求
     * @return 工序实体
     */
    @PostMapping("/projects/{projectId}/processes")
    public Result<ConstructionProcess> createProcess(@PathVariable("projectId") Long projectId,
                                                     @Valid @RequestBody ProcessCreateRequest request) {
        return Result.success(processService.create(projectId, request));
    }

    /**
     * 提交工序人工质检记录（得分达合格线记通过，否则不通过）。
     *
     * @param processId 工序ID
     * @param request   质检提交请求
     * @return 回写后的工序实体
     */
    @PostMapping("/processes/{processId}/inspect")
    public Result<ConstructionProcess> inspect(@PathVariable("processId") Long processId,
                                               @Valid @RequestBody ProcessInspectRequest request) {
        return Result.success(processService.inspect(processId, request));
    }

    /**
     * 工序 AI 视觉质检（失败降级人工复核，接口永不失败；
     * 命中违规自动落违规记录并触发区块链存证）。
     *
     * @param processId 工序ID
     * @param request   视觉质检请求
     * @return 视觉质检结果视图
     */
    @PostMapping("/processes/{processId}/vision-inspect")
    public Result<VisionInspectResultVO> visionInspect(@PathVariable("processId") Long processId,
                                                       @Valid @RequestBody VisionInspectRequest request) {
        return Result.success(processService.visionInspect(processId, request));
    }

    /* ==================== 违规与存证 ==================== */

    /**
     * 查询项目下的违规记录清单。
     *
     * @param projectId 项目ID
     * @return 违规记录列表
     */
    @GetMapping("/projects/{projectId}/violations")
    public Result<List<ConstructionViolation>> listViolations(@PathVariable("projectId") Long projectId) {
        return Result.success(violationService.listByProject(projectId));
    }

    /**
     * 人工上报违规（落违规记录并投递区块链存证，Redis 幂等防重复提交）。
     *
     * @param projectId 项目ID
     * @param request   上报请求
     * @return 违规记录
     */
    @PostMapping("/projects/{projectId}/violations")
    public Result<ConstructionViolation> reportViolation(@PathVariable("projectId") Long projectId,
                                                         @Valid @RequestBody ViolationReportRequest request) {
        return Result.success(violationService.report(projectId, request));
    }

    /**
     * 处理违规（待处理 → 已处理）。
     *
     * @param violationId 违规记录ID
     * @return 处理后的违规记录
     */
    @PutMapping("/violations/{violationId}/handle")
    public Result<ConstructionViolation> handleViolation(@PathVariable("violationId") Long violationId) {
        return Result.success(violationService.handle(violationId));
    }

    /* ==================== 施工影像 ==================== */

    /**
     * 查询项目下的影像清单（DB 只存对象键）。
     *
     * @param projectId 项目ID
     * @return 影像列表
     */
    @GetMapping("/projects/{projectId}/media")
    public Result<List<ConstructionMedia>> listMedia(@PathVariable("projectId") Long projectId) {
        return Result.success(mediaService.listByProject(projectId));
    }

    /**
     * 流式上传施工影像至 MinIO（DB 只存对象键，二进制不落本地磁盘）。
     *
     * @param projectId 项目ID
     * @param file      影像文件（multipart）
     * @param cameraId  摄像头ID（可空）
     * @return 上传结果（含对象键）
     */
    @PostMapping("/projects/{projectId}/media")
    public Result<MediaUploadVO> uploadMedia(@PathVariable("projectId") Long projectId,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "cameraId", required = false) String cameraId) {
        return Result.success(mediaService.upload(projectId, file, cameraId));
    }
}
