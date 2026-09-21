package io.github.decadedx.springaiagent.service;

/**
 * 仅供独立 RAG 问答调用的无工具聊天边界；输入已由服务层包装为非可信资料上下文。
 */
public interface RagChatClient {

    /**
     * 根据受控 Prompt 生成结构化回答文本。
     *
     * @param prompt 不包含工具定义和用户实时数据的完整 Prompt
     * @return 模型返回的 JSON 文本
     */
    String complete(String prompt);
}
