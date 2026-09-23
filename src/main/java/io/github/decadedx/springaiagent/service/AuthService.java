package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.LoginDTO;
import io.github.decadedx.springaiagent.vo.LoginVO;

/**
 * 负责验证用户凭据并签发访问令牌的认证服务。
 */
public interface AuthService {

    /**
     * 验证登录凭据并返回短期访问令牌。
     *
     * @param loginDTO 前端提交的账号与密码
     * @return 令牌及不含敏感字段的用户摘要
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 释放当前已认证账号的活动会话。
     */
    void logout();
}
