package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.enums.UserRole;

import java.security.Principal;

/**
 * 已由 JWT 验证的最小用户身份，保存业务授权所需的用户、角色与会话标识。
 *
 * @param id 可信的用户主键
 * @param role 可信的用户角色
 * @param sessionId 当前 JWT 的 Redis 会话标识；测试构造的非 HTTP 身份可为 {@code null}
 */
public record AuthenticatedUser(Long id, UserRole role, String sessionId) implements Principal {

    /**
     * 为不需要 Redis 会话的服务层测试构造最小身份。
     *
     * @param id 可信的用户主键
     * @param role 可信的用户角色
     */
    public AuthenticatedUser(Long id, UserRole role) {
        this(id, role, null);
    }

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
