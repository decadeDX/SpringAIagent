package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录接口接收的凭据；该对象只能用于认证，不得记录到日志或作为响应返回。
 */
public record LoginDTO(
        @NotBlank(message = "账号不能为空")
        @Size(max = 64, message = "账号长度不能超过64个字符")
        String username,
        @NotBlank(message = "密码不能为空")
        @Size(max = 128, message = "密码长度不能超过128个字符")
        String password
) {
}
