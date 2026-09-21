package io.github.decadedx.springaiagent.exception;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 将 Controller 和 Service 抛出的异常转换为统一、无敏感细节的 API 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 返回领域服务明确声明的业务失败。
     *
     * @param exception 业务异常
     * @return 对应状态和统一错误体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Result.failure(exception.getCode(), exception.getMessage()));
    }

    /**
     * 返回 DTO Bean Validation 的第一个字段错误，避免暴露内部校验实现。
     *
     * @param exception 参数校验异常
     * @return 400 统一错误体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数不合法" : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(Result.failure(ApiCode.BAD_REQUEST, message));
    }

    /**
     * 将无法解析的 JSON 请求体转换为统一的参数错误。
     *
     * @param exception 请求体解析异常
     * @return 400 统一错误体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Result.failure(ApiCode.BAD_REQUEST, "请求体格式不正确"));
    }

    /**
     * 将超过知识文档上传限制的 multipart 请求转换为稳定的文件大小错误。
     *
     * @param exception 上传体大小超限异常
     * @return 413 统一错误体
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.failure(ApiCode.FILE_TOO_LARGE, "上传文件不能超过 5 MiB"));
    }

    /**
     * 隐藏未预期异常的堆栈与基础设施细节。
     *
     * @param exception 未预期异常
     * @return 500 统一错误体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ApiCode.INTERNAL_ERROR, "系统繁忙，请稍后重试"));
    }
}
