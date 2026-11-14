package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 预警定位聚合结果 DTO（服务间共享）。
 */
@Data
public class WarningLocationResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 查询作用域标识 */
    private String scope;

    /** 定位条目数 */
    private long count;

    /** 定位条目列表 */
    private List<WarningLocationItemDTO> items = new ArrayList<>();

    /** 是否命中 Redis 缓存 */
    private boolean cached;

    /** 是否发生降级 */
    private boolean degraded;

    /** 降级说明 */
    private List<String> degradedSources = new ArrayList<>();
}
