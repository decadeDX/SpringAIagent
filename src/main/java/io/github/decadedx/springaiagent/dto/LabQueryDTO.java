package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 实验室列表筛选条件；未传分页参数时由服务使用第 1 页、每页 20 条。
 */
public record LabQueryDTO(
        @Size(max = 128, message = "实验室名称长度不能超过128个字符")
        String name,
        @Size(max = 255, message = "设备关键词长度不能超过255个字符")
        String equipment,
        @Min(value = 1, message = "最低容量至少为1")
        Integer minCapacity,
        @Min(value = 1, message = "页码至少为1")
        Integer page,
        @Min(value = 1, message = "每页数量至少为1")
        @Max(value = 50, message = "每页数量不能超过50")
        Integer size
) {
}
