package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 管理员查询脱敏 Agent 审计记录的分页条件。
 */
public record AgentTraceQueryDTO(
        @Size(max = 64, message = "请求标识不能超过64个字符") String requestId,
        @Size(max = 64, message = "会话标识不能超过64个字符") String sessionId,
        @Min(value = 1, message = "页码至少为1") Integer page,
        @Min(value = 1, message = "每页数量至少为1") @Max(value = 50, message = "每页数量不能超过50") Integer size
) {
}
