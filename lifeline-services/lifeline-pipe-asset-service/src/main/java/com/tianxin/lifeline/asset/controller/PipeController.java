package com.tianxin.lifeline.asset.controller;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.asset.dto.GeoJsonResponse;
import com.tianxin.lifeline.asset.dto.PipeArchiveResponse;
import com.tianxin.lifeline.asset.dto.PipeCreateRequest;
import com.tianxin.lifeline.asset.dto.PipeDetailResponse;
import com.tianxin.lifeline.asset.dto.PipePageRequest;
import com.tianxin.lifeline.asset.service.DeviceService;
import com.tianxin.lifeline.asset.service.PipeAssetService;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管网资产控制器。
 *
 * <p>提供管网分页/详情/新增/更新、GeoJSON 空间输出、一管一档等业务接口，
 * 并兼容 lifeline-service-api 中 PipeAssetFeignClient 的服务间调用契约。
 * 控制器只做参数校验与 Result 包装，业务逻辑下沉 Service。
 */
@RestController
@RequestMapping("/api/v1/pipes")
public class PipeController {

    /** 管网资产服务 */
    private final PipeAssetService pipeAssetService;

    /** 设备服务（供 /pipes/{pipeId}/devices 复用） */
    private final DeviceService deviceService;

    /**
     * 构造管网控制器。
     *
     * @param pipeAssetService 管网资产服务
     * @param deviceService    设备服务
     */
    public PipeController(PipeAssetService pipeAssetService, DeviceService deviceService) {
        this.pipeAssetService = pipeAssetService;
        this.deviceService = deviceService;
    }

    /**
     * 分页查询管网（支持 pipeType/area/status 模糊筛选，默认 20/页）。
     *
     * @param request 分页查询请求（query 参数绑定）
     * @return 管网 DTO 分页结果
     */
    @GetMapping
    public Result<PageResult<PipeDTO>> pagePipes(@Valid PipePageRequest request) {
        return Result.success(pipeAssetService.pagePipes(request));
    }

    /**
     * 获取管网空间数据（RFC 7946 FeatureCollection，Redis 缓存 TTL 10 分钟）。
     *
     * <p>注意：本端点需在 {@code /{pipeId}} 之前注册，避免被路径变量吞掉。
     *
     * @param pipeType 管网类型（可空，空为全量）
     * @return GeoJSON 响应
     */
    @GetMapping("/geojson")
    public Result<GeoJsonResponse> geoJson(@RequestParam(value = "pipeType", required = false) String pipeType) {
        return Result.success(pipeAssetService.getGeoJson(pipeType));
    }

    /**
     * 按管网类型批量查询管网（Feign 契约端点，签名与 PipeAssetFeignClient 一致）。
     *
     * @param pipeType 管网类型编码（大小写不敏感）
     * @return 管网 DTO 列表
     */
    @GetMapping("/by-type")
    public Result<List<PipeDTO>> listPipesByType(@RequestParam("pipeType") String pipeType) {
        return Result.success(pipeAssetService.listPipesByType(pipeType));
    }

    /**
     * 获取管网详情（Feign 契约端点，签名与 PipeAssetFeignClient 一致）。
     *
     * @param pipeId 管网ID
     * @return 管网 DTO
     */
    @GetMapping("/{pipeId}")
    public Result<PipeDTO> getPipeById(@PathVariable("pipeId") Long pipeId) {
        return Result.success(pipeAssetService.getPipeById(pipeId));
    }

    /**
     * 获取管网本地扩展详情（含扩展属性 KV）。
     *
     * @param pipeId 管网ID
     * @return 管网详情响应
     */
    @GetMapping("/{pipeId}/detail")
    public Result<PipeDetailResponse> getPipeDetail(@PathVariable("pipeId") Long pipeId) {
        return Result.success(pipeAssetService.getPipeDetail(pipeId));
    }

    /**
     * 查询某管网上挂载的所有设备（Feign 契约端点，签名与 PipeAssetFeignClient 一致）。
     *
     * @param pipeId 管网ID
     * @return 设备 DTO 列表
     */
    @GetMapping("/{pipeId}/devices")
    public Result<List<DeviceDTO>> listDevicesByPipeId(@PathVariable("pipeId") Long pipeId) {
        return Result.success(deviceService.listDevicesByPipeId(pipeId));
    }

    /**
     * 一管一档：主档 + 扩展属性 + 挂载设备 + 关联井室 + 拓扑关系聚合。
     *
     * @param pipeId 管网ID
     * @return 档案聚合响应
     */
    @GetMapping("/{pipeId}/archive")
    public Result<PipeArchiveResponse> getArchive(@PathVariable("pipeId") Long pipeId) {
        return Result.success(pipeAssetService.getArchive(pipeId));
    }

    /**
     * 新增管网（pipeCode 唯一，geomWkt 经 ST_GeomFromText 落库，attributes 写入 pipe_attribute）。
     *
     * @param request 新增请求
     * @return 新管网ID
     */
    @PostMapping
    public Result<Long> createPipe(@Valid @RequestBody PipeCreateRequest request) {
        return Result.success(pipeAssetService.createPipe(request));
    }

    /**
     * 更新管网（含几何与扩展属性，非空字段才更新）。
     *
     * @param pipeId  管网ID
     * @param request 更新请求
     * @return 操作结果
     */
    @PutMapping("/{pipeId}")
    public Result<Void> updatePipe(@PathVariable("pipeId") Long pipeId,
                                   @Valid @RequestBody PipeCreateRequest request) {
        pipeAssetService.updatePipe(pipeId, request);
        return Result.success();
    }
}
