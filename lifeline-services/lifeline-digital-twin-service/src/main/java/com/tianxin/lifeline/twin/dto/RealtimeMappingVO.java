package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 实时映射聚合结果（POST /realtime-mapping）。
 *
 * <p>Redis Pipeline 批量读取 {@code rt:{deviceId}}，单键失败仅跳过不中断。
 */
@Data
public class RealtimeMappingVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请求解析出的设备总数（去重后） */
    private long total;

    /** 成功命中实时缓存的设备数 */
    private long matched;

    /** 未命中（缓存不存在或读取失败跳过）的设备数 */
    private long missed;

    /** 实际分页数（每页上限 500） */
    private int pages;

    /** 设备实时状态数组 */
    private List<DeviceRealtimeVO> devices = new ArrayList<>();

    /** 是否发生降级（Redis 整体不可用或区域设备解析失败） */
    private boolean degraded;

    /** 降级说明 */
    private List<String> degradedSources = new ArrayList<>();
}
