package com.tianxin.lifeline.warning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 预警统计视图对象。
 *
 * <p>按等级 / 管网类型 / 状态三个维度分组统计预警事件数量。
 */
@Data
public class WarningStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预警总数 */
    private Long total;

    /** 按等级分组（含中文名称） */
    private List<GroupItem> byLevel;

    /** 按管网类型分组 */
    private List<GroupItem> byType;

    /** 按状态分组（含中文名称） */
    private List<GroupItem> byStatus;

    /**
     * 分组统计项。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 分组键（等级编码 / 管网类型 / 状态中文） */
        private String key;

        /** 分组展示名 */
        private String label;

        /** 数量 */
        private Long count;
    }
}
