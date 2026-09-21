package io.github.decadedx.springaiagent.dto;

import io.github.decadedx.springaiagent.enums.ReservationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * 当前用户预约列表筛选条件；起止时间表示与预约区间相交的查询窗口。
 */
public record ReservationQueryDTO(
        ReservationStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime to,
        @Min(value = 1, message = "页码至少为1")
        Integer page,
        @Min(value = 1, message = "每页数量至少为1")
        @Max(value = 50, message = "每页数量不能超过50")
        Integer size
) {
}
