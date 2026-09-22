package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.ActionExecutionStatus;
import io.github.decadedx.springaiagent.enums.ActionType;

import java.util.Map;

/**
 * 动作确认完成或成功重放后的可公开结果。
 */
public record ActionExecutionVO(String actionId, ActionType actionType, ActionExecutionStatus executionStatus,
                                boolean idempotentReplay, Map<String, Object> result) {

    /** 防止上层改写已持久化结果。 */
    public ActionExecutionVO {
        result = Map.copyOf(result);
    }
}
