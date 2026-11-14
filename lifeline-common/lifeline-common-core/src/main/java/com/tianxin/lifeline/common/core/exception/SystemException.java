package com.tianxin.lifeline.common.core.exception;

import com.tianxin.lifeline.common.core.result.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 系统异常。
 *
 * <p>用于非预期的系统级错误（中间件故障、远程调用失败、数据不一致等），
 * 由全局异常处理器捕获后记录完整堆栈并返回通用错误响应。
 */
@Getter
public class SystemException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 系统错误码 */
    private final int code;

    /**
     * 使用错误描述构造系统异常。
     *
     * @param message 错误描述
     */
    public SystemException(String message) {
        super(message);
        this.code = ErrorCode.COMMON_ERROR.getCode();
    }

    /**
     * 使用错误描述与原始异常构造系统异常。
     *
     * @param message 错误描述
     * @param cause   原始异常
     */
    public SystemException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.COMMON_ERROR.getCode();
    }

    /**
     * 使用错误码枚举与原始异常构造系统异常。
     *
     * @param errorCode 错误码枚举
     * @param cause     原始异常
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义错误码与描述构造系统异常。
     *
     * @param code    错误码
     * @param message 错误描述
     */
    public SystemException(int code, String message) {
        super(message);
        this.code = code;
    }
}
