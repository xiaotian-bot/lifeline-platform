package com.tianxin.lifeline.api.blockchain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 存证防篡改校验结果 DTO（服务间共享）。
 */
@Data
public class EvidenceVerifyDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 存证记录ID */
    private Long evidenceId;

    /** 是否被篡改 */
    private Boolean tampered;

    /** 落库存证哈希 */
    private String storedHash;

    /** 重算存证哈希 */
    private String recomputedHash;

    /** 存证哈希算法 */
    private String hashAlgorithm;

    /** 上链状态：0 待上链 / 1 已上链 */
    private Integer chainStatus;
}
