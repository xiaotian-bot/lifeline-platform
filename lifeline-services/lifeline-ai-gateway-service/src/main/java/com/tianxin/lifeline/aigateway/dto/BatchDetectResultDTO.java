package com.tianxin.lifeline.aigateway.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量异常检测汇总结果（网关内部 DTO）。
 *
 * <p>items 与入参请求列表顺序一一对应。
 */
@Data
public class BatchDetectResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请求总数 */
    private Integer total;

    /** 成功条数 */
    private Integer successCount;

    /** 降级条数 */
    private Integer degradedCount;

    /** 逐条结果（与请求顺序对齐） */
    private List<BatchDetectItemDTO> items;
}
