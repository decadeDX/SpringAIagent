package io.github.decadedx.springaiagent.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 为每个请求创建或透传安全的追踪标识，并写回响应头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** 客户端可透传的请求标识格式，避免日志字段注入。 */
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /** 追踪标识使用的请求和响应头名称。 */
    public static final String HEADER_NAME = "X-Request-Id";

    /**
     * 在请求链进入安全过滤器前设置追踪上下文，并始终清理线程本地状态。
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
        String requestId = resolveRequestId(request.getHeader(HEADER_NAME));
        RequestIdContext.set(requestId);
        MDC.put("requestId", requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestIdContext.clear();
        }
    }

    /**
     * 仅复用格式合法的客户端标识，其余情况生成服务端 UUID。
     *
     * @param incomingRequestId 客户端提供的标识
     * @return 可安全记录和返回的请求标识
     */
    private String resolveRequestId(String incomingRequestId) {
        return incomingRequestId != null && REQUEST_ID_PATTERN.matcher(incomingRequestId).matches()
                ? incomingRequestId
                : UUID.randomUUID().toString();
    }
}
