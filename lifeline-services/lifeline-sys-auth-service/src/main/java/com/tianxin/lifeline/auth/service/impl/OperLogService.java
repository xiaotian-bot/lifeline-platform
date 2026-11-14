package com.tianxin.lifeline.auth.service.impl;

import com.tianxin.lifeline.auth.entity.SysOperLog;
import com.tianxin.lifeline.auth.mapper.SysOperLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作审计日志异步记录器。
 *
 * <p>以 {@code @Async} 异步方式写入 sys_oper_log，
 * 避免审计落库阻塞登录等主流程。
 */
@Slf4j
@Component
public class OperLogService {

    /** 操作日志 Mapper */
    private final SysOperLogMapper operLogMapper;

    /**
     * 构造记录器。
     *
     * @param operLogMapper 操作日志 Mapper
     */
    public OperLogService(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    /**
     * 异步记录一条操作日志。
     *
     * @param userId   操作用户ID（可为空）
     * @param module   业务模块
     * @param action   操作动作
     * @param method   调用方法签名
     * @param params   请求参数（脱敏后）
     * @param ip       客户端IP
     * @param success  是否成功
     * @param errorMsg 错误信息（失败时）
     */
    @Async
    public void record(Long userId, String module, String action, String method,
                       String params, String ip, boolean success, String errorMsg) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setUserId(userId);
            operLog.setModule(module);
            operLog.setAction(action);
            operLog.setMethod(method);
            operLog.setParams(params);
            operLog.setIp(ip);
            operLog.setResult(success ? SysOperLog.RESULT_SUCCESS : SysOperLog.RESULT_FAIL);
            operLog.setErrorMsg(errorMsg);
            operLog.setOperTime(LocalDateTime.now());
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            // 审计日志写入失败不影响主流程
            log.warn("写入操作日志失败 module={}, action={}: {}", module, action, e.getMessage());
        }
    }
}
