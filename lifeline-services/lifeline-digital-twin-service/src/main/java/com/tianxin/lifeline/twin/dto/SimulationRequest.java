package com.tianxin.lifeline.twin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 仿真推演提交请求（POST /simulation）。
 *
 * <p>提交后立即返回 taskId，经 GET /simulation/{taskId} 轮询状态与结果。
 * 推演为基于规则的轻量场景影响评估，不落 PG（任务状态由 Redis 承载）。
 */
@Data
public class SimulationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 场景类型：LEAKAGE 泄漏 / BURST 爆管 / PRESSURE_ABNORMAL 压力异常 / THIRD_PARTY_DAMAGE 第三方破坏 */
    @NotBlank(message = "场景类型不能为空")
    @Size(max = 64, message = "场景类型长度不能超过64")
    private String scenarioType;

    /** 关联管网类型（WATER/HEAT/GAS/SEWAGE/HAZARD，可空） */
    private String pipeType;

    /** 关联预警等级（BLUE/YELLOW/ORANGE/RED，可空，用于影响范围放大系数） */
    private String warningLevel;

    /** 关联管网ID（可空，存在时输出更精确的隔离区段建议） */
    private Long pipeId;

    /** 附加参数（如触发值/阈值/管径等，参与规则评估） */
    private Map<String, Object> params;
}
