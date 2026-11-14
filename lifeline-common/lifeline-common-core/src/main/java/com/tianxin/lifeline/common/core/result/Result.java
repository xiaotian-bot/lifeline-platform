package com.tianxin.lifeline.common.core.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应体。
 *
 * <p>全平台所有 RESTful 接口统一使用该结构返回，格式：
 * <pre>
 * { "code": 200, "message": "success", "data": {}, "timestamp": 1724000000000 }
 * </pre>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 成功提示语 */
    public static final String SUCCESS_MESSAGE = "success";

    /** 响应码，200 表示成功，其余为错误码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 服务端响应时间戳（毫秒） */
    private long timestamp;

    /**
     * 默认构造，自动填充服务端时间戳。
     */
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功响应（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage(SUCCESS_MESSAGE);
        result.setData(data);
        return result;
    }

    /**
     * 失败响应（使用通用错误码）。
     *
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String message) {
        return fail(ErrorCode.COMMON_ERROR.getCode(), message);
    }

    /**
     * 失败响应（使用错误码枚举）。
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    /**
     * 失败响应（自定义错误码与描述）。
     *
     * @param code    错误码
     * @param message 错误描述
     * @param <T>     数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /**
     * 判断当前响应是否成功。
     *
     * @return true 表示成功
     */
    public boolean isSuccess() {
        return this.code == SUCCESS_CODE;
    }
}
