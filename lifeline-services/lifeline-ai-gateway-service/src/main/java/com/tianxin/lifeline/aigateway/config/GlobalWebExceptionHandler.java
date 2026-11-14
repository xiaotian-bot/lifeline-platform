package com.tianxin.lifeline.aigateway.config;

import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.core.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * 全局异常兜底（WebFlux 注解控制器）。
 *
 * <p>网关对外承诺「不抛 5xx」：上游失败已在 AiCapabilityTemplate 内降级；
 * 此处兜住请求体解析失败等其余异常，统一返回 HTTP 200 + Result 错误码，
 * 避免默认错误页泄露堆栈与 5xx 状态。
 */
@Slf4j
@RestControllerAdvice
public class GlobalWebExceptionHandler {

    /**
     * 兜底所有未处理异常。
     *
     * @param e 异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public Mono<Result<Void>> handleException(Exception e) {
        log.warn("AI网关请求处理异常（已兜底, 不抛5xx）: {}", e.getMessage());
        return Mono.just(Result.fail(ErrorCode.AI_INVOKE_ERROR.getCode(),
                "AI网关请求处理失败: " + e.getMessage()));
    }
}
