package com.tianxin.lifeline.blockchain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * DID 注册请求。
 *
 * <p>同一 pipeId 幂等：已注册返回既有 DID，不重复生成。
 */
@Data
public class DidRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网ID（幂等键） */
    @NotNull(message = "管网ID不能为空")
    private Long pipeId;

    /** 管网编码（参与 DID 生成） */
    @NotBlank(message = "管网编码不能为空")
    private String pipeCode;
}
