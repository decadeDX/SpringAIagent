package io.github.decadedx.springaiagent.common;

/**
 * 定义后端内部业务语义与对外整数响应码的唯一映射；Controller 和 Service 只使用枚举名称，
 * 客户端只接收 {@link #value()} 返回的数字。
 */
public enum ApiCode {

    OK(200),
    BAD_REQUEST(40000),
    REPAIR_REQUIRED_FIELD_MISSING(40001),
    UNAUTHENTICATED(40100),
    SESSION_INVALIDATED(40101),
    FORBIDDEN(40300),
    RESERVATION_NOT_OWNED(40301),
    NOT_FOUND(40400),
    LAB_NOT_FOUND(40401),
    REPAIR_TICKET_NOT_FOUND(40402),
    BUSINESS_CONFLICT(40900),
    RESERVATION_CONFLICT(40901),
    RESERVATION_LIMIT_REACHED(40902),
    ACTION_DRAFT_EXPIRED(40903),
    INVALID_TICKET_TRANSITION(40904),
    ACCOUNT_ALREADY_LOGGED_IN(40905),
    FILE_TOO_LARGE(41300),
    UNSUPPORTED_MEDIA_TYPE(41500),
    BUSINESS_RULE_VIOLATION(42200),
    LAB_MAINTENANCE(42201),
    TRAINING_REQUIRED(42202),
    INVALID_RESERVATION_TIME(42203),
    CANCELLATION_TOO_LATE(42204),
    RESOLUTION_NOTE_REQUIRED(42205),
    RATE_LIMITED(42900),
    INTERNAL_ERROR(50000),
    DEPENDENCY_UNAVAILABLE(50300),
    REDIS_UNAVAILABLE(50301),
    DEPENDENCY_TIMEOUT(50400);

    /** 对外稳定的整数响应码。 */
    private final int value;

    /**
     * 创建内部语义码与外部响应码的映射。
     *
     * @param value 对外整数响应码
     */
    ApiCode(int value) {
        this.value = value;
    }

    /**
     * 返回写入 JSON 响应体的整数错误码。
     *
     * @return 对外整数响应码
     */
    public int value() {
        return value;
    }
}
