package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 实时映射聚合结果 DTO（服务间共享）。
 */
@Data
public class RealtimeMappingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请求解析出的设备总数 */
    private long total;

    /** 成功命中实时缓存的设备数 */
    private long matched;

    /** 未命中的设备数 */
    private long missed;

    /** 实际分页数 */
    private int pages;

    /** 设备实时状态数组 */
    private List<DeviceRealtimeDTO> devices = new ArrayList<>();

    /** 是否发生降级 */
    private boolean degraded;

    /** 降级说明 */
    private List<String> degradedSources = new ArrayList<>();
}
