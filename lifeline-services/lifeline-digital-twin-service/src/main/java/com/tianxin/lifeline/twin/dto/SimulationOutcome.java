package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 仿真推演结构化结果（规则推演输出）。
 *
 * <p>按预警等级/管线类型估算影响范围与建议隔离区段，序列化为 JSON 存 Redis。
 */
@Data
public class SimulationOutcome implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 场景类型（回显） */
    private String scenarioType;

    /** 管网类型（回显，可空） */
    private String pipeType;

    /** 预警等级（回显，可空） */
    private String warningLevel;

    /** 影响等级：LOW/MEDIUM/HIGH/CRITICAL */
    private String impactLevel;

    /** 估算影响半径（米） */
    private double impactRadiusMeters;

    /** 估算影响面积（平方米，按圆形近似） */
    private double impactAreaSquareMeters;

    /** 建议隔离区段描述列表 */
    private List<String> isolationSegments = new ArrayList<>();

    /** 处置建议列表 */
    private List<String> suggestions = new ArrayList<>();

    /** 结论摘要 */
    private String summary;
}
