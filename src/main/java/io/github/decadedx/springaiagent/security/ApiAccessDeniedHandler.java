package io.github.decadedx.springaiagent.security;

import tools.jackson.databind.ObjectMapper;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.common.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 将已认证但权限不足的请求统一转换为不泄露资源细节的 403 JSON 响应。
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    /** 用于写出统一 JSON 响应的序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建授权失败处理器。
     *
     * @param objectMapper JSON 序列化器
     */
    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 返回统一 403 响应，不暴露 Spring Security 的内部授权规则。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param accessDeniedException Spring Security 授权异常
     * @throws IOException 响应写入异常
     * @throws ServletException Servlet 处理异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.failure(ApiCode.FORBIDDEN, "无权访问该资源"));
    }
}
