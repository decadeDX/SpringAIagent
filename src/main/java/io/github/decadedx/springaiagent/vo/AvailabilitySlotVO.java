package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

/**
 * 一个左闭右开的整点可用性时隙；结果仅供展示，最终可用性由预约确认事务决定。
 *
 * @param startTime 时隙开始时间，固定使用 +08:00 偏移输出
 * @param endTime 时隙结束时间，固定使用 +08:00 偏移输出
 * @param available 当前是否未被已确认预约占用
 */
public record AvailabilitySlotVO(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime endTime,
        boolean available
) {
}
