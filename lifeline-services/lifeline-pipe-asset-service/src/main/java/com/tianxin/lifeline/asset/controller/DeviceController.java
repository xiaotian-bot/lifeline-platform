package com.tianxin.lifeline.asset.controller;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.asset.dto.DevicePageRequest;
import com.tianxin.lifeline.asset.service.DeviceService;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管网设备控制器。
 *
 * <p>提供设备分页/详情接口，并兼容 lifeline-service-api 中
 * PipeAssetFeignClient 的服务间调用契约。
 * 控制器只做参数校验与 Result 包装，业务逻辑下沉 Service。
 */
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    /** 设备服务 */
    private final DeviceService deviceService;

    /**
     * 构造设备控制器。
     *
     * @param deviceService 设备服务
     */
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 分页查询设备（支持 deviceType/pipeId/status/keyword 筛选，默认 20/页）。
     *
     * @param request 分页查询请求（query 参数绑定）
     * @return 设备 DTO 分页结果
     */
    @GetMapping
    public Result<PageResult<DeviceDTO>> pageDevices(@Valid DevicePageRequest request) {
        return Result.success(deviceService.pageDevices(request));
    }

    /**
     * 根据设备ID获取设备档案（Feign 契约端点，签名与 PipeAssetFeignClient 一致）。
     *
     * @param deviceId 设备ID
     * @return 设备 DTO
     */
    @GetMapping("/{deviceId}")
    public Result<DeviceDTO> getDeviceById(@PathVariable("deviceId") Long deviceId) {
        return Result.success(deviceService.getDeviceById(deviceId));
    }
}
