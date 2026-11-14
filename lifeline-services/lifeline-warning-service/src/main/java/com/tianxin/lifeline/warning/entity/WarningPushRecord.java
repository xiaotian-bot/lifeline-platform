package com.tianxin.lifeline.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 预警推送记录实体（对应表 warning_push_record）。
 *
 * <p>预警事件按等级匹配 warning_level.push_scope 后逐接收人生成推送记录，
 * 经 Kafka {@code pipe_warning_push} 异步化后由 WarningPushListener 落库留痕。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_push_record")
public class WarningPushRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 推送状态：待发送 */
    public static final int STATUS_PENDING = 0;

    /** 推送状态：已发送 */
    public static final int STATUS_SENT = 1;

    /** 关联预警事件ID */
    private Long eventId;

    /** 推送渠道：sms/app/wechat/voice/screen */
    private String channel;

    /** 接收对象（角色或人员名称） */
    private String receiver;

    /** 推送内容 */
    private String content;

    /** 推送状态：0 待发送 / 1 已发送 */
    private Integer status;

    /** 推送时间 */
    private LocalDateTime pushTime;
}
