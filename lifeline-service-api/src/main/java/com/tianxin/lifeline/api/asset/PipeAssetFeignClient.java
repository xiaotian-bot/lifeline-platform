package com.tianxin.lifeline.api.asset;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.GeoJsonDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 管网资产服务（pipe-asset-service）Feign 客户端。
 *
 * <p>供其他微服务查询管网档案与附属设备信息。
 */
@FeignClient(name = "pipe-asset-service", path = "/api/v1")
public interface PipeAssetFeignClient {

    /**
     * 获取管网详情。
     *
     * @param pipeId 管网ID
     * @return 管网信息
     */
    @GetMapping("/pipes/{pipeId}")
    Result<PipeDTO> getPipeById(@PathVariable("pipeId") Long pipeId);

    /**
     * 按管网类型批量查询管网。
     *
     * @param pipeType 管网类型编码（WATER/HEAT/GAS/SEWAGE/HAZARD）
     * @return 管网列表
     */
    @GetMapping("/pipes/by-type")
    Result<List<PipeDTO>> listPipesByType(@RequestParam("pipeType") String pipeType);

    /**
     * 获取管网 GeoJSON FeatureCollection（pipe-asset 侧 Redis 缓存 TTL 10 分钟）。
     *
     * @param pipeType 管网类型编码（可空，空时返回全部类型）
     * @return GeoJSON FeatureCollection
     */
    @GetMapping("/pipes/geojson")
    Result<GeoJsonDTO> getGeoJson(@RequestParam(value = "pipeType", required = false) String pipeType);

    /**
     * 根据设备ID获取设备档案。
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    @GetMapping("/devices/{deviceId}")
    Result<DeviceDTO> getDeviceById(@PathVariable("deviceId") Long deviceId);

    /**
     * 查询某管网上挂载的所有设备。
     *
     * @param pipeId 管网ID
     * @return 设备列表
     */
    @GetMapping("/pipes/{pipeId}/devices")
    Result<List<DeviceDTO>> listDevicesByPipeId(@PathVariable("pipeId") Long pipeId);
}
