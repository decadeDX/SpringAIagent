package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

/**
 * 当前用户创建的短期聊天会话摘要。
 */
public record ChatSessionVO(String sessionId, String name,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime createdAt) {
}
