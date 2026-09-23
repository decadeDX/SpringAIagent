package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.enums.UserRole;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 签发并解析本系统的 HS256 访问令牌，不访问数据库也不保存会话状态。
 */
@Component
public class JwtTokenService {

    /** 负责为已验证用户签名 JWT 的编码器。 */
    private final JwtEncoder jwtEncoder;

    /** 负责校验签名、issuer 和到期时间的解码器。 */
    private final JwtDecoder jwtDecoder;

    /** JWT 的有效期和 issuer 配置。 */
    private final JwtProperties jwtProperties;

    /**
     * 创建令牌服务。
     *
     * @param jwtEncoder HS256 编码器
     * @param jwtDecoder HS256 解码器
     * @param jwtProperties JWT 配置
     */
    public JwtTokenService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtProperties = jwtProperties;
    }

    /**
     * 为已完成密码认证的用户签发两小时访问令牌。
     *
     * @param userId 数据库用户主键
     * @param role 已验证的角色
     * @param sessionId Redis 中唯一活动会话的随机标识
     * @return 令牌文本和到期时间
     */
    public IssuedJwtToken issue(Long userId, UserRole role, String sessionId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.getTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", role.name())
                .claim("sid", sessionId)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedJwtToken(token, expiresAt);
    }

    /**
     * 校验令牌签名、过期时间和 issuer，并读取可信用户身份。
     *
     * @param token Bearer Token 中的 JWT 文本
     * @return 已验证的用户 ID 与角色
     * @throws JwtException 令牌无效、过期或 Claim 不合法时抛出
     */
    public AuthenticatedUser parse(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        try {
            Long userId = Long.valueOf(jwt.getSubject());
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            String sessionId = jwt.getClaimAsString("sid");
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("JWT 会话声明无效");
            }
            return new AuthenticatedUser(userId, role, sessionId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new JwtException("JWT 用户身份声明无效", exception);
        }
    }
}
