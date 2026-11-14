package com.tianxin.lifeline.blockchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证请求（REST 入参）。
 *
 * <p>服务端按口径 {@code hash(content | relatedId | relatedType | 时间戳)}
 * 计算存证哈希后落库上链。
 */
@Data
public class EvidenceRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证类型（如 ORDER 工单 / CONSTRUCTION 施工 / PLAN 预案 / WARNING 预警） */
    @NotBlank(message = "存证类型不能为空")
    private String evidenceType;

    /** 关联业务对象ID（溯源聚合键，如管网ID/工单ID） */
    @NotNull(message = "关联业务对象ID不能为空")
    private Long relatedId;

    /** 关联业务对象类型（如 PIPE 管网 / ORDER 工单） */
    @NotBlank(message = "关联业务对象类型不能为空")
    private String relatedType;

    /** 存证内容（建议 JSON 文本，描述业务环节关键要素） */
    private String content;
}
