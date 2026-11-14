package com.tianxin.lifeline.asset.service;

import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.asset.dto.GeoJsonResponse;
import com.tianxin.lifeline.asset.dto.PipeArchiveResponse;
import com.tianxin.lifeline.asset.dto.PipeCreateRequest;
import com.tianxin.lifeline.asset.dto.PipeDetailResponse;
import com.tianxin.lifeline.asset.dto.PipePageRequest;
import com.tianxin.lifeline.common.core.page.PageResult;

import java.util.List;

/**
 * 管网资产服务。
 *
 * <p>职责：管网台账分页/详情/新增/更新、GeoJSON 空间输出（Redis 缓存）、
 * 一管一档聚合，并对外提供 Feign 契约所需数据。
 */
public interface PipeAssetService {

    /**
     * 分页查询管网（支持类型/区域关键字/状态筛选）。
     *
     * @param request 分页查询请求
     * @return 管网 DTO 分页结果
     */
    PageResult<PipeDTO> pagePipes(PipePageRequest request);

    /**
     * 获取管网详情（Feign 契约口径）。
     *
     * @param pipeId 管网ID
     * @return 管网 DTO
     */
    PipeDTO getPipeById(Long pipeId);

    /**
     * 获取管网本地扩展详情（含扩展属性 KV）。
     *
     * @param pipeId 管网ID
     * @return 管网详情响应
     */
    PipeDetailResponse getPipeDetail(Long pipeId);

    /**
     * 按管网类型查询列表（大小写不敏感）。
     *
     * @param pipeType 管网类型编码
     * @return 管网 DTO 列表
     */
    List<PipeDTO> listPipesByType(String pipeType);

    /**
     * 新增管网（pipeCode 唯一校验，几何与扩展属性一并落库）。
     *
     * @param request 新增请求
     * @return 新管网ID
     */
    Long createPipe(PipeCreateRequest request);

    /**
     * 更新管网（含几何与扩展属性，非空字段才更新）。
     *
     * @param pipeId  管网ID
     * @param request 更新请求
     */
    void updatePipe(Long pipeId, PipeCreateRequest request);

    /**
     * 获取管网 GeoJSON FeatureCollection（Redis 缓存，TTL 10 分钟）。
     *
     * @param pipeType 管网类型（可空，空为全量）
     * @return GeoJSON 响应
     */
    GeoJsonResponse getGeoJson(String pipeType);

    /**
     * 一管一档：聚合主档、扩展属性、挂载设备、关联井室与拓扑关系。
     *
     * @param pipeId 管网ID
     * @return 档案聚合响应
     */
    PipeArchiveResponse getArchive(Long pipeId);
}
