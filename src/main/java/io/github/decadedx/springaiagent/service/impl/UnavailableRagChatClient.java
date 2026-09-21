package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.service.RagChatClient;

/**
 * 在未启用 OpenAI 配置时拒绝模型调用，避免产生脱离资料的本地伪回答。
 */
public class UnavailableRagChatClient implements RagChatClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public String complete(String prompt) {
        throw new IllegalStateException("知识库模型服务未启用");
    }
}
