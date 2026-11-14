package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI预案生成代理结果 DTO（服务间共享）。
 *
 * <p>包含预案名称、处置任务清单、资源需求清单与综合处置建议。
 */
@Data
public class PlanAgentResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 预案名称 */
    private String planName;

    /** 处置任务清单 */
    private List<PlanTaskItemDTO> tasks;

    /** 资源需求清单 */
    private List<PlanResourceItemDTO> resources;

    /** 综合处置建议 */
    private String suggestion;
}
