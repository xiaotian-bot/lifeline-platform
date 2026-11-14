package com.tianxin.lifeline.api.construction;

import com.tianxin.lifeline.api.construction.dto.ConstructionMediaDTO;
import com.tianxin.lifeline.api.construction.dto.ConstructionProcessDTO;
import com.tianxin.lifeline.api.construction.dto.ConstructionProjectDTO;
import com.tianxin.lifeline.api.construction.dto.ConstructionProjectDetailDTO;
import com.tianxin.lifeline.api.construction.dto.ConstructionViolationDTO;
import com.tianxin.lifeline.api.construction.dto.NearbyPipeDTO;
import com.tianxin.lifeline.api.construction.dto.ProcessCreateRequestDTO;
import com.tianxin.lifeline.api.construction.dto.ProcessInspectRequestDTO;
import com.tianxin.lifeline.api.construction.dto.ProjectCreateRequestDTO;
import com.tianxin.lifeline.api.construction.dto.ViolationReportRequestDTO;
import com.tianxin.lifeline.api.construction.dto.VisionInspectRequestDTO;
import com.tianxin.lifeline.api.construction.dto.VisionInspectResultDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 施工监护服务（construction-service）Feign 客户端。
 *
 * <p>供预警/数字孪生等上游服务查询施工项目、工序质检与违规记录。
 * <p>注：影像上传接口（multipart）不适合跨服务 Feign 调用，未纳入。
 */
@FeignClient(name = "construction-service", path = "/api/v1/constructions")
public interface ConstructionFeignClient {

    /* ==================== 施工项目 ==================== */

    /**
     * 分页查询施工项目。
     *
     * @param status 项目状态
     * @param area   区域/关键字
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    @GetMapping("/projects")
    Result<PageResult<ConstructionProjectDTO>> pageProjects(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "area", required = false) String area,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size);

    /**
     * 创建施工项目。
     *
     * @param request 创建请求
     * @return 项目视图对象
     */
    @PostMapping("/projects")
    Result<ConstructionProjectDTO> createProject(@RequestBody ProjectCreateRequestDTO request);

    /**
     * 查询施工项目详情。
     *
     * @param id 项目ID
     * @return 详情聚合视图
     */
    @GetMapping("/projects/{id}")
    Result<ConstructionProjectDetailDTO> projectDetail(@PathVariable("id") Long id);

    /**
     * 更新施工项目。
     *
     * @param id      项目ID
     * @param request 更新请求
     * @return 项目视图对象
     */
    @PutMapping("/projects/{id}")
    Result<ConstructionProjectDTO> updateProject(@PathVariable("id") Long id,
                                                 @RequestBody ProjectCreateRequestDTO request);

    /**
     * 逻辑删除施工项目。
     *
     * @param id 项目ID
     * @return 空结果
     */
    @DeleteMapping("/projects/{id}")
    Result<Void> deleteProject(@PathVariable("id") Long id);

    /**
     * 推进项目状态。
     *
     * @param id     项目ID
     * @param target 目标状态
     * @return 流转后的项目视图对象
     */
    @PostMapping("/projects/{id}/status")
    Result<ConstructionProjectDTO> advanceStatus(@PathVariable("id") Long id,
                                                 @RequestParam("target") Integer target);

    /**
     * 周边管线影响范围查询。
     *
     * @param id           项目ID
     * @param radiusMeters 影响半径（米）
     * @param limit        最大返回条数
     * @return 周边管线列表
     */
    @GetMapping("/projects/{id}/nearby-pipes")
    Result<List<NearbyPipeDTO>> nearbyPipes(
            @PathVariable("id") Long id,
            @RequestParam(value = "radiusMeters", defaultValue = "200") double radiusMeters,
            @RequestParam(value = "limit", defaultValue = "50") int limit);

    /* ==================== 施工工序与质检 ==================== */

    /**
     * 查询项目下的工序清单。
     *
     * @param projectId 项目ID
     * @return 工序列表
     */
    @GetMapping("/projects/{projectId}/processes")
    Result<List<ConstructionProcessDTO>> listProcesses(@PathVariable("projectId") Long projectId);

    /**
     * 创建工序。
     *
     * @param projectId 项目ID
     * @param request   工序创建请求
     * @return 工序实体
     */
    @PostMapping("/projects/{projectId}/processes")
    Result<ConstructionProcessDTO> createProcess(@PathVariable("projectId") Long projectId,
                                                 @RequestBody ProcessCreateRequestDTO request);

    /**
     * 提交工序人工质检记录。
     *
     * @param processId 工序ID
     * @param request   质检提交请求
     * @return 回写后的工序实体
     */
    @PostMapping("/processes/{processId}/inspect")
    Result<ConstructionProcessDTO> inspect(@PathVariable("processId") Long processId,
                                           @RequestBody ProcessInspectRequestDTO request);

    /**
     * 工序 AI 视觉质检。
     *
     * @param processId 工序ID
     * @param request   视觉质检请求
     * @return 视觉质检结果视图
     */
    @PostMapping("/processes/{processId}/vision-inspect")
    Result<VisionInspectResultDTO> visionInspect(@PathVariable("processId") Long processId,
                                                 @RequestBody VisionInspectRequestDTO request);

    /* ==================== 违规与存证 ==================== */

    /**
     * 查询项目下的违规记录清单。
     *
     * @param projectId 项目ID
     * @return 违规记录列表
     */
    @GetMapping("/projects/{projectId}/violations")
    Result<List<ConstructionViolationDTO>> listViolations(@PathVariable("projectId") Long projectId);

    /**
     * 人工上报违规。
     *
     * @param projectId 项目ID
     * @param request   上报请求
     * @return 违规记录
     */
    @PostMapping("/projects/{projectId}/violations")
    Result<ConstructionViolationDTO> reportViolation(@PathVariable("projectId") Long projectId,
                                                     @RequestBody ViolationReportRequestDTO request);

    /**
     * 处理违规。
     *
     * @param violationId 违规记录ID
     * @return 处理后的违规记录
     */
    @PutMapping("/violations/{violationId}/handle")
    Result<ConstructionViolationDTO> handleViolation(@PathVariable("violationId") Long violationId);

    /* ==================== 影像 ==================== */

    /**
     * 查询项目下的影像清单。
     *
     * @param projectId 项目ID
     * @return 影像列表
     */
    @GetMapping("/projects/{projectId}/media")
    Result<List<ConstructionMediaDTO>> listMedia(@PathVariable("projectId") Long projectId);
}
