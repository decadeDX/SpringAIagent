package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.LoginDTO;
import io.github.decadedx.springaiagent.service.AuthService;
import io.github.decadedx.springaiagent.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供匿名登录入口；登录成功后客户端使用返回的 Bearer Token 访问业务 API。
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
}
