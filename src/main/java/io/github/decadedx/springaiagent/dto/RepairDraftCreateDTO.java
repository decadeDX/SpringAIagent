package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.Size;

/**
 * 报修草案的原始输入；用户身份由当前认证上下文确定，不接受 userId。
 *
 * @param labId 故障所在实验室业务编号
 * @param equipmentInfo 故障设备名称或资产编号
 * @param description 故障现象说明
 * @param safetyRisk 学生主动标记的风险信号，可被服务端高风险规则提升为 true
 */
public record RepairDraftCreateDTO(
        @Size(max = 32, message = "实验室编号不能超过32个字符") String labId,
        @Size(max = 255, message = "设备信息不能超过255个字符") String equipmentInfo,
        @Size(max = 10000, message = "故障描述不能超过10000个字符") String description,
        Boolean safetyRisk
) {
}
