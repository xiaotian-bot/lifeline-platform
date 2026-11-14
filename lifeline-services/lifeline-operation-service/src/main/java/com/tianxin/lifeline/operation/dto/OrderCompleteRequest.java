package com.tianxin.lifeline.operation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单完成请求（POST /api/v1/orders/{id}/complete 请求体）。
 */
@Data
public class OrderCompleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 处置结果描述（必填） */
    @NotBlank(message = "处置结果不能为空")
    private String result;
}
