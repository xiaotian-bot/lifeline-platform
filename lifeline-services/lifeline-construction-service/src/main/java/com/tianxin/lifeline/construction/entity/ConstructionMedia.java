package com.tianxin.lifeline.construction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 施工影像实体（对应表 construction_media）。
 *
 * <p>影像二进制经 MinIO 流式上传，本表只存对象键
 * （file_url = MinIO object key），不落存储地址。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("construction_media")
public class ConstructionMedia extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属施工项目ID */
    private Long projectId;

    /** 影像类型：image 图片 / video 视频 */
    private String mediaType;

    /** MinIO 对象键（DB 只存对象键，不存二进制） */
    private String fileUrl;

    /** 拍摄/采集时间 */
    private LocalDateTime captureTime;

    /** 摄像头ID（视频流采集场景） */
    private String cameraId;
}
