package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 一轮 Agent 对话的回答、真实来源与可选待确认草案。
 */
public record ChatMessageVO(String sessionId, String messageId, String answer,
                            List<KnowledgeCitationVO> citations, int toolCallCount, ActionDraftVO draft) {

    /** 确保响应引用不能被调用方修改。 */
    public ChatMessageVO {
        citations = List.copyOf(citations);
    }
}
