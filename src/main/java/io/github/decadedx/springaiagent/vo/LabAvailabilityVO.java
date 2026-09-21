package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 一天内实验室开放时段和可查询的整点时隙。
 *
 * @param labId 实验室业务编号
 * @param date 按上海时区解释的日期
 * @param openTime 实验室每日开放时间
 * @param closeTime 实验室每日关闭时间
 * @param slots 在请求范围内的整点时隙
 */
public record LabAvailabilityVO(String labId, LocalDate date,
                                @JsonFormat(pattern = "HH:mm") LocalTime openTime,
                                @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
                                List<AvailabilitySlotVO> slots) {
}
