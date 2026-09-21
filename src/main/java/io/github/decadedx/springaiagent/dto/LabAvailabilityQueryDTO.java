package io.github.decadedx.springaiagent.dto;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 查询一天内实验室整点时隙的条件；时间范围按 Asia/Shanghai 当地时间解释。
 */
public record LabAvailabilityQueryDTO(
        @NotNull(message = "日期不能为空")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,
        @DateTimeFormat(pattern = "HH:mm")
        LocalTime from,
        @DateTimeFormat(pattern = "HH:mm")
        LocalTime to
) {
}
