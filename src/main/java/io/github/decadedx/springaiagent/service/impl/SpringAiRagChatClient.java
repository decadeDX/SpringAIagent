package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.service.RagChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * 基于 Spring AI ChatClient 的 OpenAI 问答适配器；不配置工具或对话记忆。
 */
public class SpringAiRagChatClient implements RagChatClient {

    /** 只用于当前 RAG 请求的一次性聊天客户端。 */
    private final ChatClient chatClient;

    /**
     * 创建 OpenAI RAG 聊天适配器。
     *
     * @param chatModel Spring AI 提供的聊天模型
     */
    public SpringAiRagChatClient(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String complete(String prompt) {
        return chatClient.prompt().user(prompt).call().content();
    }
}
