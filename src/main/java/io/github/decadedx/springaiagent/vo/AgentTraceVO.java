package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.OffsetDateTime;

/**
 * 管理员可查看的脱敏工具审计记录，不包含原始用户输入、密钥或思维链。
 */
public record AgentTraceVO(@JsonSerialize(using = ToStringSerializer.class) Long id, String requestId,
                           String sessionId, String toolName, String resultSummary,
                           int durationMs, String errorCode,
                           @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime createdAt) {
}
