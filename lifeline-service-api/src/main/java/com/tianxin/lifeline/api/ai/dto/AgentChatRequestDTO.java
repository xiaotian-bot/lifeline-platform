package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 运维问答代理请求 DTO（服务间共享）。
 *
 * <p>转发 agent-service POST /api/v1/ai/agent/chat，
 * 面向运维人员的管网安全知识问答。
 */
@Data
public class AgentChatRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户问题 */
    private String question;

    /** 会话上下文（可选，自由文本/历史摘要） */
    private String context;
}
