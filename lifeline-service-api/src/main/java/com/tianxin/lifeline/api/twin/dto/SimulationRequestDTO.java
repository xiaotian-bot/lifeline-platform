package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 仿真推演提交请求 DTO（服务间共享）。
 */
@Data
public class SimulationRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 场景类型：LEAKAGE / BURST / PRESSURE_ABNORMAL / THIRD_PARTY_DAMAGE */
    private String scenarioType;

    /** 关联管网类型 */
    private String pipeType;

    /** 关联预警等级 */
    private String warningLevel;

    /** 关联管网ID */
    private Long pipeId;

    /** 附加参数 */
    private Map<String, Object> params;
}
