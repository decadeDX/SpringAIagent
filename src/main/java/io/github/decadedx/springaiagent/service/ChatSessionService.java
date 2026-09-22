package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.vo.ChatSessionVO;

import java.util.List;

/**
 * 保存当前用户隔离的短期 Agent 对话上下文。
 */
public interface ChatSessionService {

    /**
     * 创建新会话。
     *
     * @param name 可选展示名称
     * @return 新会话摘要
     */
    ChatSessionVO create(String name);

    /**
     * 读取当前用户拥有的会话及其有限消息历史。
     *
     * @param sessionId 会话标识
     * @return 当前会话
     */
    StoredChatSession require(String sessionId);

    /**
     * 追加一条消息并续期会话。
     *
     * @param sessionId 会话标识
     * @param role 消息角色
     * @param content 消息内容
     */
    void append(String sessionId, String role, String content);

    /**
     * Redis 中会话的最小序列化结构。
     *
     * @param schemaVersion 缓存结构版本
     * @param userId 所属用户
     * @param sessionId 会话标识
     * @param name 展示名称
     * @param createdAt 创建时刻
     * @param messages 受限消息历史
     */
    record StoredChatSession(int schemaVersion, Long userId, String sessionId, String name,
                             java.time.OffsetDateTime createdAt, List<ChatEntry> messages) {
        /** 防止调用者改写保存的上下文。 */
        public StoredChatSession {
            messages = List.copyOf(messages);
        }
    }

    /**
     * 会话中一条最小消息，角色只能由服务端写入。
     *
     * @param role user 或 assistant
     * @param content 可用于下一轮模型上下文的文本
     */
    record ChatEntry(String role, String content) {
    }
}
