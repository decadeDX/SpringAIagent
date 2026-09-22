package io.github.decadedx.springaiagent.dto;

import io.github.decadedx.springaiagent.enums.LabStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

/**
 * 管理员修改实验室基础资料的白名单输入；未传字段保留当前数据库值。
 */
public record LabUpdateDTO(
        @Size(max = 128, message = "实验室名称不能超过128个字符") String name,
        @Min(value = 1, message = "实验室容量至少为1") Integer capacity,
        @Size(max = 10000, message = "设备说明不能超过10000个字符") String equipmentDescription,
        @DateTimeFormat(pattern = "HH:mm") LocalTime openTime,
        @DateTimeFormat(pattern = "HH:mm") LocalTime closeTime,
        LabStatus status
) {
}
