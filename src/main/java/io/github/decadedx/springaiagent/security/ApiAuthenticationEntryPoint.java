package io.github.decadedx.springaiagent.security;

import tools.jackson.databind.ObjectMapper;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.common.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 将认证或会话依赖失败统一转换为不泄露令牌细节的 JSON 响应。
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** 用于写出统一 JSON 响应的序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建认证失败处理器。
     *
     * @param objectMapper JSON 序列化器
     */
    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 返回统一 401 响应，不将底层 JWT 解析原因暴露给客户端。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param authException Spring Security 认证异常
     * @throws IOException 响应写入异常
     * @throws ServletException Servlet 处理异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        writeResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                Result.failure(ApiCode.UNAUTHENTICATED, "请先登录或重新登录"));
    }

    /**
     * 返回会话已被释放或失效的统一 401 响应，供前端清理本地登录状态。
     *
     * @param response HTTP 响应
     * @throws IOException 响应写入异常
     */
    public void commenceSessionInvalidated(HttpServletResponse response) throws IOException {
        writeResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                Result.failure(ApiCode.SESSION_INVALIDATED, "登录已失效，请重新登录"));
    }

    /**
     * Redis 不可用时拒绝认证请求，避免绕过单账号会话校验。
     *
     * @param response HTTP 响应
     * @throws IOException 响应写入异常
     */
    public void commenceRedisUnavailable(HttpServletResponse response) throws IOException {
        writeResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                Result.failure(ApiCode.REDIS_UNAVAILABLE, "登录服务暂不可用，请稍后重试"));
    }

    /**
     * 写出统一 JSON 失败响应。
     *
     * @param response HTTP 响应
     * @param status HTTP 状态码
     * @param body 失败响应体
     * @throws IOException 响应写入异常
     */
    private void writeResponse(HttpServletResponse response, int status, Result<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
