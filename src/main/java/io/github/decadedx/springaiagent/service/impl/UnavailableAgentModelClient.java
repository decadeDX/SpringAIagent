package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.service.AgentModelClient;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 未启用真实模型时拒绝聊天，避免退化为关键词拼接或伪造业务完成状态。
 */
public class UnavailableAgentModelClient implements AgentModelClient {

    /** {@inheritDoc} */
    @Override
    public String reply(String systemPrompt, List<String> history, String message, Object tools) {
        throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.DEPENDENCY_UNAVAILABLE,
                "智能助手暂不可用，请先配置模型与知识库服务");
    }
}
