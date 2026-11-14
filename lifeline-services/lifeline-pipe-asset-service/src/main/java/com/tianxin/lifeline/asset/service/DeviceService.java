package com.tianxin.lifeline.asset.service;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.asset.dto.DevicePageRequest;
import com.tianxin.lifeline.common.core.page.PageResult;

import java.util.List;

/**
 * 管网设备服务。
 *
 * <p>职责：设备详情/挂载设备查询、设备分页，并对外提供 Feign 契约所需数据。
 */
public interface DeviceService {

    /**
     * 获取设备详情（含经纬度与所属管网类型）。
     *
     * @param deviceId 设备ID
     * @return 设备 DTO
     */
    DeviceDTO getDeviceById(Long deviceId);

    /**
     * 查询某管网上挂载的全部设备。
     *
     * @param pipeId 管网ID
     * @return 设备 DTO 列表
     */
    List<DeviceDTO> listDevicesByPipeId(Long pipeId);

    /**
     * 分页查询设备（支持类型/管网/状态/关键字筛选）。
     *
     * @param request 分页查询请求
     * @return 设备 DTO 分页结果
     */
    PageResult<DeviceDTO> pageDevices(DevicePageRequest request);
}
