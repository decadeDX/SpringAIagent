package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.ChatMessageDTO;
import io.github.decadedx.springaiagent.vo.ChatMessageVO;

/**
 * 编排一轮受限模型工具调用、会话上下文和可验证响应。
 */
public interface AgentOrchestrationService {

    /**
     * 向当前用户拥有的会话发送消息。
     *
     * @param sessionId 会话标识
     * @param messageDTO 用户消息
     * @return 一轮 Agent 结果
     */
    ChatMessageVO send(String sessionId, ChatMessageDTO messageDTO);
}
