package io.github.decadedx.springaiagent.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * REST 接口统一响应体；失败响应省略 data，且始终携带请求追踪标识。
 *
 * @param code 稳定的整数业务或协议错误码
 * @param message 面向调用方的简短消息
 * @param data 成功时的业务数据
 * @param requestId 本次请求的追踪标识
 * @param <T> 成功数据类型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(int code, String message, T data, String requestId) {

    /** 成功响应的固定业务码。 */
    private static final int SUCCESS_CODE = ApiCode.OK.value();

    /** 成功响应的固定消息。 */
    private static final String SUCCESS_MESSAGE = "成功";

    /**
     * 创建包含当前请求标识的成功响应。
     *
     * @param data 业务数据
     * @return 标准成功响应
     * @param <T> 业务数据类型
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data, RequestIdContext.currentOrCreate());
    }

    /**
     * 创建包含当前请求标识的失败响应。
     *
     * @param code 内部稳定错误码
     * @param message 面向用户的错误消息
     * @return 不含 data 的标准失败响应
     */
    public static Result<Void> failure(ApiCode code, String message) {
        return new Result<>(code.value(), message, null, RequestIdContext.currentOrCreate());
    }
}
