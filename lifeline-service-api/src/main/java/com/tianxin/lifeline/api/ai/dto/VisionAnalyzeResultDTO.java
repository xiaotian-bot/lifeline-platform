package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI视觉分析结果 DTO（服务间共享）。
 *
 * <p>视觉大模型对施工影像的违规识别结论，命中违规时由调用方
 * 落库 construction_violation 并触发区块链存证。
 */
@Data
public class VisionAnalyzeResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否识别到违规行为 */
    private Boolean violation;

    /** 违规类型：如 NO_HELMET 未戴安全帽 / NO_FENCE 围挡缺失 / UNAUTHORIZED_EXCAVATION 擅自开挖 */
    private String violationType;

    /** 识别置信度（0-1） */
    private Double confidence;

    /** 证据影像URL（标注/截取后的证据文件地址） */
    private String evidenceUrl;

    /** 违规描述 */
    private String description;
}
