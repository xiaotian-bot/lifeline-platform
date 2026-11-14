package com.tianxin.lifeline.api.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * DID 注册请求 DTO（服务间共享）。
 */
@Data
public class DidRegisterRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网ID（幂等键） */
    private Long pipeId;

    /** 管网编码（参与 DID 生成） */
    private String pipeCode;
}
