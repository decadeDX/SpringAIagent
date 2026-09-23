package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.LoginDTO;
import io.github.decadedx.springaiagent.service.AuthService;
import io.github.decadedx.springaiagent.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供登录、会话检查与退出入口；登录成功后客户端使用返回的 Bearer Token 访问业务 API。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 负责验证凭据并签发令牌的认证服务。 */
    private final AuthService authService;

    /**
     * 创建认证控制器。
     *
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 验证账号密码并返回短期 Bearer Token。
     *
     * @param loginDTO 登录请求体
     * @return 令牌与脱敏用户摘要
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /**
     * 释放当前 JWT 对应的 Redis 会话，使该账号可在其他设备重新登录。
     *
     * @return 不含业务数据的成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success(null);
    }

    /**
     * 供前端心跳确认当前会话仍有效；认证过滤器已完成会话校验后才会进入此方法。
     *
     * @return 不含业务数据的成功响应
     */
    @GetMapping("/session")
    public Result<Void> session() {
        return Result.success(null);
    }
}
