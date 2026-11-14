package com.tianxin.lifeline.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DID 注册结果视图。
 */
@Data
public class DidVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** DID 记录ID */
    private Long id;

    /** DID 标识（did:lifeline:{pipeCode}:{随机后缀}） */
    private String did;

    /** 管网ID */
    private Long pipeId;

    /** 管网编码 */
    private String pipeCode;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 是否命中幂等（true 表示返回既有 DID，未新注册） */
    private Boolean reused;
}
