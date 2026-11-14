package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI视觉分析请求 DTO（服务间共享）。
 *
 * <p>施工监管场景下，将现场摄像头影像提交 ai-gateway-service
 * 进行违规行为识别分析。
 */
@Data
public class VisionAnalyzeRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 摄像头ID */
    private String cameraId;

    /** 关联施工项目ID */
    private Long projectId;

    /** 影像文件URL（MinIO 对象地址） */
    private String mediaUrl;

    /** 影像类型：image 图片 / video 视频 */
    private String mediaType;
}
