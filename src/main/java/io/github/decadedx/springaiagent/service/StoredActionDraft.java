package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.enums.ActionType;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Redis 中序列化的草案结构；schemaVersion 允许未来安全地拒绝不兼容的缓存数据。
 */
public record StoredActionDraft(int schemaVersion, String actionId, Long userId, String sessionId,
                                ActionType actionType, JsonNode payload, List<String> notices,
                                OffsetDateTime expiresAt) {

    /** 限制提示集合为不可变，避免调用方篡改后续确认信息。 */
    public StoredActionDraft {
        notices = List.copyOf(notices);
    }
}
