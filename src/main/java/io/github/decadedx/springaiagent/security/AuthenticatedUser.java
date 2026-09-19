package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.enums.UserRole;

import java.security.Principal;

/**
 * 已由 JWT 验证的最小用户身份，仅保存业务服务必需的用户 ID 与角色。
 *
 * @param id 可信的用户主键
 * @param role 可信的用户角色
 */
public record AuthenticatedUser(Long id, UserRole role) implements Principal {

    /**
     * 以用户 ID 作为 Spring Security 的主体名称，避免使用可变的账号名参与授权。
     *
     * @return 用户 ID 的字符串形式
     */
    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
