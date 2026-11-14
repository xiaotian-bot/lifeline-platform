package com.tianxin.lifeline.api.ai.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 运维问答代理结果 DTO（服务间共享）。
 *
 * <p>agent-service POST /api/v1/ai/agent/chat 的出参契约。
 */
@Data
public class AgentChatResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 问答回复内容 */
    private String answer;

    /** 引用来源（知识库条目/规范文档等） */
    private List<String> references;
}
