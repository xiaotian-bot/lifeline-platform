package com.tianxin.lifeline.operation.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 运维工单统计视图对象。
 *
 * <p>提供按状态与按工单类型两个维度的分组统计。
 */
@Data
public class OrderStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 工单总数 */
    private Long total;

    /** 按状态分组统计项 */
    private List<GroupCount> byStatus;

    /** 按工单类型分组统计项 */
    private List<GroupCount> byOrderType;

    /**
     * 分组统计项。
     */
    @Data
    public static class GroupCount implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 分组键（状态数值或类型编码） */
        private String groupKey;

        /** 分组中文说明（状态中文或类型中文） */
        private String groupText;

        /** 该分组工单数量 */
        private Long count;
    }
}
