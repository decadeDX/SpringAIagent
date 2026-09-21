package io.github.decadedx.springaiagent.dto;

/**
 * 已通过取消草案准备的目标预约标识；确认时仍须重新检查归属、状态和时间边界。
 */
public record ReservationCancelPayload(Long reservationId) {
}
