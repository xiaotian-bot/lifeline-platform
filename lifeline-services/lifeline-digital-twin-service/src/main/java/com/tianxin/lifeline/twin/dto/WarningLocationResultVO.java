package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 预警定位聚合结果（GET /warning-location）。
 *
 * <p>结果在 Redis 缓存 1 分钟（键 lifeline:twin:warning-loc:{scope}）。
 */
@Data
public class WarningLocationResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 查询作用域标识（warning-{id} 或 pipe-{id}） */
    private String scope;

    /** 定位条目数 */
    private long count;

    /** 定位条目列表 */
    private List<WarningLocationItemVO> items = new ArrayList<>();

    /** 是否命中 Redis 缓存 */
    private boolean cached;

    /** 是否发生降级 */
    private boolean degraded;

    /** 降级说明（如 warning / pipe / device / realtime） */
    private List<String> degradedSources = new ArrayList<>();
}
