package io.github.decadedx.springaiagent.dto;

import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 当前学生本人报修工单的分页筛选条件。
 *
 * @param status 可选的工单状态
 * @param page 从 1 开始的页码
 * @param size 每页返回数量
 */
public record RepairTicketQueryDTO(
        RepairTicketStatus status,
        @Min(value = 1, message = "页码至少为1") Integer page,
        @Min(value = 1, message = "每页数量至少为1")
        @Max(value = 100, message = "每页数量不能超过100") Integer size
) {
}
