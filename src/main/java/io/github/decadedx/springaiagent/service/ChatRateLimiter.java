package io.github.decadedx.springaiagent.service;

/**
 * 对聊天消息执行按用户的固定窗口限流。
 */
public interface ChatRateLimiter {

    /**
     * 消耗当前认证用户本分钟的一次聊天额度。
     */
    void check();
}
