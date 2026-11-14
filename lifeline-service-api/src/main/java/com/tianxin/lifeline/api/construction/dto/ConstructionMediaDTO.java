package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 施工影像 DTO（服务间共享）。
 *
 * <p>镜像 construction-service 的 ConstructionMedia 实体，供跨服务调用。
 */
@Data
public class ConstructionMediaDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 影像记录ID */
    private Long id;

    /** 所属施工项目ID */
    private Long projectId;

    /** 影像类型：image 图片 / video 视频 */
    private String mediaType;

    /** MinIO 对象键 */
    private String fileUrl;

    /** 拍摄/采集时间 */
    private LocalDateTime captureTime;

    /** 摄像头ID */
    private String cameraId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
