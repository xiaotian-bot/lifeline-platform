package com.tianxin.lifeline.common.web.handler;

import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.exception.SystemException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.core.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>统一拦截控制器层抛出的异常并转换为 {@link Result} 响应体：
 * <ul>
 *     <li>{@link BizException}：业务异常，直接透出错误码与描述</li>
 *     <li>{@link SystemException}：系统异常，记录堆栈，返回系统错误提示</li>
 *     <li>{@link MethodArgumentNotValidException} / {@link BindException}：参数校验失败</li>
 *     <li>{@link Exception}：兜底处理，返回通用错误</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param ex      业务异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBizException(BizException ex, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: code={}, message={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理系统异常。
     *
     * @param ex      系统异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleSystemException(SystemException ex, HttpServletRequest request) {
        log.error("系统异常 [{}] {}: code={}, message={}", request.getMethod(), request.getRequestURI(),
                ex.getCode(), ex.getMessage(), ex);
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理方法入参校验异常（@Valid + @RequestBody）。
     *
     * @param ex      校验异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理表单绑定校验异常（@Valid + 表单参数）。
     *
     * @param ex      绑定异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 [{}] {}: {}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /**
     * 处理资源不存在异常。
     *
     * @param ex      资源不存在异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("资源不存在 [{}] {}: {}", request.getMethod(), request.getRequestURI(), ex.getResourcePath());
        return Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 兜底处理未知异常。
     *
     * @param ex      未知异常
     * @param request 当前请求
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("未知异常 [{}] {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.fail(ErrorCode.COMMON_ERROR);
    }

    /**
     * 格式化字段校验错误信息。
     *
     * @param fieldError 字段错误
     * @return "字段名: 错误描述" 格式的文本
     */
    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
