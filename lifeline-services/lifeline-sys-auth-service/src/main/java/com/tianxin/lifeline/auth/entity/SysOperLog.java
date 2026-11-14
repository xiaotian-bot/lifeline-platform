package com.tianxin.lifeline.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体（对应表 sys_oper_log）。
 *
 * <p>由认证服务异步写入，记录登录、用户管理、权限分配等关键操作。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 操作结果：成功 */
    public static final int RESULT_SUCCESS = 1;

    /** 操作结果：失败 */
    public static final int RESULT_FAIL = 0;

    /** 操作用户ID */
    private Long userId;

    /** 业务模块（如：认证中心、用户管理） */
    private String module;

    /** 操作动作（如：login、create_user） */
    private String action;

    /** 调用方法签名 */
    private String method;

    /** 请求参数（脱敏后） */
    private String params;

    /** 客户端IP */
    private String ip;

    /** 操作结果：0 失败 / 1 成功 */
    private Integer result;

    /** 错误信息（失败时记录） */
    private String errorMsg;

    /** 操作时间 */
    private LocalDateTime operTime;
}
