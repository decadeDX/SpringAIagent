package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * 预约草案的原始输入；不含用户标识，服务只从认证上下文取得预约所属人。
 */
public record ReservationDraftCreateDTO(
        @NotBlank(message = "实验室编号不能为空")
        String labId,
        @NotNull(message = "开始时间不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime startTime,
        @NotNull(message = "结束时间不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime endTime,
        @NotNull(message = "参与人数不能为空")
        @Min(value = 1, message = "参与人数至少为1")
        @Max(value = 1000, message = "参与人数不能超过1000")
        Integer participantCount
) {
}
