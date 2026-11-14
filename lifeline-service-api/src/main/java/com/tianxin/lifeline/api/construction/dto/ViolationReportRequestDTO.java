package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人工违规上报请求 DTO（服务间共享）。
 */
@Data
public class ViolationReportRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 违规类型：NO_HELMET / NO_FENCE / UNAUTHORIZED_EXCAVATION 等 */
    private String violationType;

    /** 违规描述 */
    private String description;

    /** 证据影像对象键（MinIO 对象地址，可空） */
    private String evidenceUrl;
}
