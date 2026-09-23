package io.github.decadedx.springaiagent.security;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Bearer JWT 并建立当前请求的 Spring Security 身份上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Bearer Token 的标准请求头名称。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Bearer Token 的固定前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 的签发与验证服务。 */
    private final JwtTokenService jwtTokenService;

    /** 无效令牌时写出统一 401 的处理器。 */
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    /** 校验每个账号唯一 Redis 会话的服务。 */
    private final UserSessionService userSessionService;

    /**
     * 创建 JWT 认证过滤器。
     *
     * @param jwtTokenService JWT 服务
     * @param authenticationEntryPoint 认证失败处理器
     * @param userSessionService Redis 用户会话服务
     */
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   ApiAuthenticationEntryPoint authenticationEntryPoint,
                                   UserSessionService userSessionService) {
        this.jwtTokenService = jwtTokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.userSessionService = userSessionService;
    }

    /**
     * 缺少 Authorization 时交由后续授权规则处理；携带无效 Bearer Token 时立即返回 401。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param filterChain 后续过滤器链
     * @throws ServletException 过滤器链执行异常
     * @throws IOException 响应写入异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthenticatedUser user = jwtTokenService.parse(authorization.substring(BEARER_PREFIX.length()));
            if (!userSessionService.isActive(user.id(), user.sessionId())) {
                authenticationEntryPoint.commenceSessionInvalidated(response);
                return;
            }
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()))));
            SecurityContextHolder.setContext(securityContext);
            filterChain.doFilter(request, response);
        } catch (JwtException exception) {
            authenticationEntryPoint.commence(request, response, new InvalidJwtAuthenticationException(exception));
        } catch (BusinessException exception) {
            if (exception.getCode() == ApiCode.REDIS_UNAVAILABLE) {
                authenticationEntryPoint.commenceRedisUnavailable(response);
                return;
            }
            throw exception;
        }
    }

    /**
     * 将 JWT 校验失败转换为 Spring Security 可识别的认证异常，不包含原始令牌文本。
     */
    private static class InvalidJwtAuthenticationException extends org.springframework.security.core.AuthenticationException {

        /**
         * 创建不泄露令牌内容的认证异常。
         *
         * @param cause JWT 校验异常
         */
        private InvalidJwtAuthenticationException(Throwable cause) {
            super("JWT 无效", cause);
        }
    }
}
