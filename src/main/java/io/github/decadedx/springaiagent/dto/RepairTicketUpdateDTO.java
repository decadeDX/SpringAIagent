package io.github.decadedx.springaiagent.dto;

import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 管理员处理工单时提交的目标状态和本次处理说明。
 *
 * @param status 目标工单状态，服务层只接受相邻状态
 * @param resolutionNote 本次处理说明，不能只包含空白字符
 */
public record RepairTicketUpdateDTO(
        @NotNull(message = "工单状态不能为空") RepairTicketStatus status,
        @Size(max = 10000, message = "处理说明不能超过10000个字符") String resolutionNote
) {
}
