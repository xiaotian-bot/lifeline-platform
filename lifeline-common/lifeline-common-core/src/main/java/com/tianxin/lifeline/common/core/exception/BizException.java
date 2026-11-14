package com.tianxin.lifeline.common.core.exception;

import com.tianxin.lifeline.common.core.result.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>用于业务规则校验失败等可预期的场景，由全局异常处理器捕获后
 * 转换为统一响应体返回前端，不打印堆栈。
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final int code;

    /**
     * 使用错误码枚举构造业务异常。
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码枚举与自定义描述构造业务异常。
     *
     * @param errorCode 错误码枚举
     * @param message   自定义错误描述
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义错误码与描述构造业务异常。
     *
     * @param code    错误码
     * @param message 错误描述
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 仅使用描述构造业务异常（错误码取通用业务错误码）。
     *
     * @param message 错误描述
     */
    public BizException(String message) {
        super(message);
        this.code = ErrorCode.COMMON_ERROR.getCode();
    }
}
