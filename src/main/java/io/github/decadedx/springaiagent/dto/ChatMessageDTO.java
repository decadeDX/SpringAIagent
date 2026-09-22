package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户向已有会话发送的一条自然语言消息。
 */
public record ChatMessageDTO(
        @NotBlank(message = "消息不能为空")
        @Size(max = 10000, message = "消息不能超过10000个字符")
        String content
) {
}
