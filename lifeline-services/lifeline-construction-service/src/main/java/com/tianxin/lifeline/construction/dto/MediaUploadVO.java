package com.tianxin.lifeline.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 施工影像上传结果视图。
 *
 * <p>影像二进制经 MinIO 流式上传后仅返回对象键，DB 只存对象键。
 */
@Data
public class MediaUploadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 影像记录ID */
    private Long id;

    /** 所属施工项目ID */
    private Long projectId;

    /** 影像类型：image / video */
    private String mediaType;

    /** MinIO 对象键 */
    private String objectKey;

    /** 拍摄/采集时间 */
    private LocalDateTime captureTime;
}
