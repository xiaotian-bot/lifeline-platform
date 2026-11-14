package com.tianxin.lifeline.asset.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.asset.converter.AssetDtoConverter;
import com.tianxin.lifeline.asset.dto.DevicePageRequest;
import com.tianxin.lifeline.asset.entity.PipeDevice;
import com.tianxin.lifeline.asset.mapper.PipeDeviceMapper;
import com.tianxin.lifeline.asset.service.DeviceService;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管网设备服务实现。
 *
 * <p>设备坐标经 ST_X/ST_Y 提取，管网类型经 JOIN pipe_main 回填。
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    /** 管网设备 Mapper */
    private final PipeDeviceMapper pipeDeviceMapper;

    /**
     * 构建设备服务。
     *
     * @param pipeDeviceMapper 管网设备 Mapper
     */
    public DeviceServiceImpl(PipeDeviceMapper pipeDeviceMapper) {
        this.pipeDeviceMapper = pipeDeviceMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeviceDTO getDeviceById(Long deviceId) {
        PipeDevice device = pipeDeviceMapper.selectDeviceById(deviceId);
        if (device == null) {
            throw new BizException(ErrorCode.DEVICE_NOT_FOUND);
        }
        return AssetDtoConverter.toDeviceDto(device);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeviceDTO> listDevicesByPipeId(Long pipeId) {
        return pipeDeviceMapper.selectByPipeId(pipeId).stream().map(AssetDtoConverter::toDeviceDto).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<DeviceDTO> pageDevices(DevicePageRequest request) {
        Page<PipeDevice> page = new Page<>(request.getPage(), request.getSize());
        IPage<PipeDevice> result = pipeDeviceMapper.selectDevicePage(
                page, request.getDeviceType(), request.getPipeId(), request.getStatus(), request.getKeyword());
        List<DeviceDTO> records = result.getRecords().stream().map(AssetDtoConverter::toDeviceDto).toList();
        return PageResult.of(records, result.getTotal(), request.getPage(), request.getSize());
    }
}
