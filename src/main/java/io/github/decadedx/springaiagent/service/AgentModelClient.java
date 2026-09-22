package io.github.decadedx.springaiagent.service;

import java.util.List;

/**
 * 隔离 Spring AI 模型调用，便于在未配置模型和测试环境中提供确定行为。
 */
public interface AgentModelClient {

    /**
     * 使用注册工具完成一轮对话。
     *
     * @param systemPrompt 服务端系统规则
     * @param history 有界历史文本
     * @param message 当前用户消息
     * @param tools 仅允许调用的工具对象
     * @return 模型最终文本回答
     */
    String reply(String systemPrompt, List<String> history, String message, Object tools);
}
