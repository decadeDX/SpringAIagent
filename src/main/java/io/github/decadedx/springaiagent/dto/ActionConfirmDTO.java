package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户点击确认时提交的会话绑定信息；不接收用户或动作类型以避免伪造。
 */
public record ActionConfirmDTO(
        @NotBlank(message = "会话标识不能为空")
        @Size(max = 64, message = "会话标识不能超过64个字符")
        String sessionId
) {
}
