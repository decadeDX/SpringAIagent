package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.decadedx.springaiagent.enums.ReservationStatus;

import java.time.OffsetDateTime;

/**
 * 当前用户可见的预约结果，不携带预约所属用户标识。
 *
 * @param id 预约数据库主键
 * @param reservationNo 展示用预约编号
 * @param labId 实验室业务编号
 * @param labName 实验室名称
 * @param startTime 上海时区的预约开始时间
 * @param endTime 上海时区的预约结束时间
 * @param participantCount 参与人数
 * @param status 当前预约状态
 * @param cancelledAt 取消完成时间；有效预约为空
 */
public record ReservationVO(Long id, String reservationNo, String labId, String labName,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime startTime,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime endTime,
                            int participantCount, ReservationStatus status,
                            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime cancelledAt) {
}
