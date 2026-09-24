package io.github.decadedx.springaiagent.dto;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * 已通过取消草案准备的目标预约标识；确认时仍须重新检查归属、状态和时间边界。
 *
 * @param reservationId 预约主键；草案响应以字符串输出以避免浏览器精度丢失
 */
public record ReservationCancelPayload(@JsonSerialize(using = ToStringSerializer.class) Long reservationId) {
}
