package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工序 AI 视觉质检请求 DTO（服务间共享）。
 */
@Data
public class VisionInspectRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 摄像头ID */
    private String cameraId;

    /** 影像对象键/URL（MinIO 对象地址） */
    private String mediaUrl;

    /** 影像类型：image 图片 / video 视频，缺省 image */
    private String mediaType = "image";
}
