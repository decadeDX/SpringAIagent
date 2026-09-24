package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.OffsetDateTime;

/**
 * 当前学生或管理员可见的工单结果，不暴露提交人和处理人的内部主键。
 *
 * @param id 工单主键；以字符串输出，避免浏览器处理超大整数时丢失精度
 * @param ticketNo 展示用工单编号
 * @param labId 实验室业务编号
 * @param equipmentInfo 故障设备信息
 * @param description 故障说明
 * @param safetyRisk 是否为高风险工单
 * @param status 当前状态
 * @param resolutionNote 最近一次管理员处理说明
 * @param createdAt 上海时区的创建时间
 * @param updatedAt 上海时区的最近更新时间
 */
public record RepairTicketVO(@JsonSerialize(using = ToStringSerializer.class) Long id, String ticketNo, String labId,
                             String equipmentInfo, String description,
                             boolean safetyRisk, RepairTicketStatus status, String resolutionNote,
                             @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime createdAt,
                             @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime updatedAt) {
}
