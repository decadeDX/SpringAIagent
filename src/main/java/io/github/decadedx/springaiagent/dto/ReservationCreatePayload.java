package io.github.decadedx.springaiagent.dto;

import java.time.OffsetDateTime;

/**
 * 已通过草案准备的预约业务载荷；模块 05 将其与用户、会话和 actionId 绑定后再请求最终确认。
 */
public record ReservationCreatePayload(
        String labId,
        String labName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        int participantCount
) {
}
