package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 时序异常检测结果 DTO（服务间共享）。
 */
@Data
public class AnomalyResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long deviceId;

    /** 指标类型 */
    private String metricType;

    /** 是否异常 */
    private Boolean anomaly;

    /** 异常得分（0-1，越大越异常） */
    private Double anomalyScore;

    /** 异常类型：如 LEAK/BURST/CORROSION/BLOCKAGE */
    private String anomalyType;

    /** 置信度（0-1） */
    private Double confidence;

    /** 异常点索引（在输入序列中的位置） */
    private List<Integer> anomalyIndexes;

    /** 模型名称 */
    private String model;

    /** 推理耗时（毫秒） */
    private Long costMs;
}
