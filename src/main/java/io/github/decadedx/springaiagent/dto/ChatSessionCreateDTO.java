package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.Size;

/**
 * 创建聊天会话的可选展示名称。
 */
public record ChatSessionCreateDTO(@Size(max = 128, message = "会话名称不能超过128个字符") String name) {
}
