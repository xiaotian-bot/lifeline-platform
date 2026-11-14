package com.tianxin.lifeline.construction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人工违规上报请求。
 *
 * <p>上报即落违规记录并经 EventEnvelope 投递 pipe_evidence_request
 * 触发区块链存证。
 */
@Data
public class ViolationReportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 违规类型：NO_HELMET / NO_FENCE / UNAUTHORIZED_EXCAVATION 等 */
    @NotBlank(message = "违规类型不能为空")
    @Size(max = 64, message = "违规类型长度不能超过64")
    private String violationType;

    /** 违规描述 */
    @NotBlank(message = "违规描述不能为空")
    private String description;

    /** 证据影像对象键（MinIO 对象地址，可空） */
    @Size(max = 512, message = "证据影像地址长度不能超过512")
    private String evidenceUrl;
}
