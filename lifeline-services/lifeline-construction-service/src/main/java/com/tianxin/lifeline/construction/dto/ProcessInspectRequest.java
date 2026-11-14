package com.tianxin.lifeline.construction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 工序人工质检记录提交请求。
 *
 * <p>质检得分达到合格线（60 分）记为质检通过，否则记为质检不通过。
 */
@Data
public class ProcessInspectRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 质检人 */
    @NotBlank(message = "质检人不能为空")
    @Size(max = 64, message = "质检人长度不能超过64")
    private String inspector;

    /** 质检得分（百分制，0-100） */
    @NotNull(message = "质检得分不能为空")
    @DecimalMin(value = "0.0", message = "质检得分不能小于0")
    @DecimalMax(value = "100.0", message = "质检得分不能大于100")
    private BigDecimal qualityScore;
}
