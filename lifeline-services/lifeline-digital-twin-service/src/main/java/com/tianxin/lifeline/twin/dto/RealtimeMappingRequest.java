package com.tianxin.lifeline.twin.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 实时映射请求（POST /realtime-mapping）。
 *
 * <p>{@code deviceIds} 与 {@code areaId} 二选一（至少其一非空）：
 * 传 deviceIds 直接批量映射；传 areaId 时先经 pipe-asset 解析该区域设备再映射。
 * 单批次上限 500 设备，超限由服务端分页处理。
 */
@Data
public class RealtimeMappingRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码列表（如 DEV-GAS-001），与 areaId 二选一 */
    @Size(max = 5000, message = "单次请求设备数不能超过5000")
    private List<String> deviceIds;

    /** 区域ID，与 deviceIds 二选一（经 pipe-asset 解析区域内设备） */
    private Long areaId;
}
