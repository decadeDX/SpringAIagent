package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.ActionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户在有效期内可核对并确认的临时动作，不代表业务事实已经写入。
 */
public record ActionDraftVO(String actionId, ActionType actionType, String sessionId, OffsetDateTime expiresAt,
                            Map<String, Object> payload, List<String> notices) {

    /** 防止调用者改写展示用集合。 */
    public ActionDraftVO {
        payload = Map.copyOf(payload);
        notices = List.copyOf(notices);
    }
}
