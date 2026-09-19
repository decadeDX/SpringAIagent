package io.github.decadedx.springaiagent.exception;

import io.github.decadedx.springaiagent.common.ApiCode;
import org.springframework.http.HttpStatus;

/**
 * 表示可安全返回调用方的预期业务失败，并保留对应 HTTP 状态与稳定错误码。
 */
public class BusinessException extends RuntimeException {

    /** 失败响应使用的 HTTP 状态。 */
    private final HttpStatus status;

    /** 供前端和测试稳定判断的错误码。 */
    private final ApiCode code;

    /**
     * 创建业务异常。
     *
     * @param status HTTP 状态
     * @param code 内部稳定错误码
     * @param message 面向调用方的错误消息
     */
    public BusinessException(HttpStatus status, ApiCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * 返回该失败应映射的 HTTP 状态。
     *
     * @return HTTP 状态
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 返回稳定错误码。
     *
     * @return 错误码
     */
    public ApiCode getCode() {
        return code;
    }
}
