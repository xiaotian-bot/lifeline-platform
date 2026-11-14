package com.tianxin.lifeline.construction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工序 AI 视觉质检请求。
 *
 * <p>将现场影像提交 ai-gateway-service 视觉分析，识别违规并回写
 * 工序质检记录；命中违规时落违规记录并触发区块链存证。
 */
@Data
public class VisionInspectRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 摄像头ID */
    @Size(max = 64, message = "摄像头ID长度不能超过64")
    private String cameraId;

    /** 影像对象键/URL（MinIO 对象地址） */
    @NotBlank(message = "影像地址不能为空")
    @Size(max = 512, message = "影像地址长度不能超过512")
    private String mediaUrl;

    /** 影像类型：image 图片 / video 视频，缺省 image */
    @Pattern(regexp = "image|video", message = "影像类型仅支持 image / video")
    private String mediaType = "image";
}
