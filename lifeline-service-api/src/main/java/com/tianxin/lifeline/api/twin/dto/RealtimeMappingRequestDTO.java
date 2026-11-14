package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 实时映射请求 DTO（服务间共享）。
 */
@Data
public class RealtimeMappingRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码列表，与 areaId 二选一 */
    private List<String> deviceIds;

    /** 区域ID，与 deviceIds 二选一 */
    private Long areaId;
}
