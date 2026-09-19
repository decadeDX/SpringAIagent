package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.decadedx.springaiagent.dto.LoginDTO;
import io.github.decadedx.springaiagent.entity.SysUser;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.IssuedJwtToken;
import io.github.decadedx.springaiagent.security.JwtTokenService;
import io.github.decadedx.springaiagent.service.AuthService;
import io.github.decadedx.springaiagent.vo.LoginVO;
import io.github.decadedx.springaiagent.vo.UserSummaryVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 使用数据库 BCrypt 摘要验证登录凭据，并在成功后签发短期访问令牌。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /** 系统用户的持久化访问入口。 */
    private final SysUserMapper sysUserMapper;

    /** BCrypt 密码摘要比对器。 */
    private final PasswordEncoder passwordEncoder;

    /** 已验证用户的 JWT 签发服务。 */
    private final JwtTokenService jwtTokenService;

    /**
     * 创建认证服务。
     *
     * @param sysUserMapper 用户 Mapper
     * @param passwordEncoder BCrypt 密码编码器
     * @param jwtTokenService JWT 服务
     */
    public AuthServiceImpl(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder,
                           JwtTokenService jwtTokenService) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 使用账号唯一查询用户并比对 BCrypt 摘要；失败时不泄露账号是否存在。
     *
     * @param loginDTO 登录凭据
     * @return JWT、到期时间和脱敏用户摘要
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.username()));
        if (user == null || !passwordEncoder.matches(loginDTO.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "账号或密码错误");
        }

        IssuedJwtToken token = jwtTokenService.issue(user.getId(), user.getRole());
        return new LoginVO(
                token.value(),
                "Bearer",
                OffsetDateTime.ofInstant(token.expiresAt(), ZoneId.of("Asia/Shanghai")),
                new UserSummaryVO(user.getId(), user.getUsername(), user.getRole(), user.getTrainingStatus()));
    }
}
