package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 业务服务读取已验证操作者的唯一入口，禁止从请求参数或聊天内容读取用户身份。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * 返回当前已认证用户的数据库主键。
     *
     * @return 可信用户主键
     */
    public static Long requireId() {
        return requireAuthenticatedUser().id();
    }

    /**
     * 返回当前已认证用户的角色。
     *
     * @return 可信用户角色
     */
    public static UserRole requireRole() {
        return requireAuthenticatedUser().role();
    }

    /**
     * 从 Spring Security 上下文提取由 JWT 过滤器创建的最小身份。
     *
     * @return 已认证用户
     */
    private static AuthenticatedUser requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHENTICATED, "请先登录");
        }
        return user;
    }
}
