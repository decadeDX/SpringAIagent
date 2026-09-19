package io.github.decadedx.springaiagent.common;

import java.util.UUID;

/**
 * 保存当前 HTTP 请求的追踪标识，供成功和异常响应统一引用。
 */
public final class RequestIdContext {

    /** 当前线程正在处理的请求标识。 */
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestIdContext() {
    }

    /**
     * 返回当前请求标识；在非 HTTP 调用场景中按需生成新的标识。
     *
     * @return 当前调用关联的请求标识
     */
    public static String currentOrCreate() {
        String requestId = REQUEST_ID.get();
        return requestId == null ? UUID.randomUUID().toString() : requestId;
    }

    /**
     * 绑定过滤器已确认安全的请求标识。
     *
     * @param requestId 本次请求的追踪标识
     */
    public static void set(String requestId) {
        REQUEST_ID.set(requestId);
    }

    /** 清理线程本地状态，避免 Tomcat 线程复用时串联请求。 */
    public static void clear() {
        REQUEST_ID.remove();
    }
}
