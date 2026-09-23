package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApplicationMetrics;
import io.github.decadedx.springaiagent.service.AgentModelClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

/**
 * 使用 Spring AI ChatClient 执行受控工具调用，不向模型暴露数据库或 Redis 访问能力。
 */
public class SpringAiAgentModelClient implements AgentModelClient {

    /** Spring AI 聊天客户端。 */
    private final ChatClient chatClient;

    /** 模型调用耗时指标。 */
    private final ApplicationMetrics applicationMetrics;

    /**
     * 创建真实模型适配器。
     *
     * @param chatModel 自动配置的聊天模型
     * @param applicationMetrics 模型调用指标
     */
    public SpringAiAgentModelClient(ChatModel chatModel, ApplicationMetrics applicationMetrics) {
        this.chatClient = ChatClient.create(chatModel);
        this.applicationMetrics = applicationMetrics;
    }

    /** {@inheritDoc} */
    @Override
    public String reply(String systemPrompt, List<String> history, String message, Object tools) {
        String historyText = history.isEmpty() ? "无" : String.join("\n", history);
        long startedAt = System.nanoTime();
        try {
            return chatClient.prompt()
                    .system(systemPrompt + "\n\n【有限会话历史】\n" + historyText)
                    .user(message)
                    .tools(tools)
                    .call()
                    .content();
        } finally {
            applicationMetrics.modelCall(System.nanoTime() - startedAt);
        }
    }
}
