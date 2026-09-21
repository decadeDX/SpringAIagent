package io.github.decadedx.springaiagent.dto;

import java.util.List;

/**
 * 已通过草案校验、可由模块 05 在确认事务中恢复的报修创建载荷。
 *
 * @param labId 已确认存在的实验室业务编号
 * @param labName 草案展示使用的实验室名称，不作为确认阶段的信任依据
 * @param equipmentInfo 已校验的设备信息
 * @param description 已校验的故障说明
 * @param safetyRisk 服务端最终确定的风险标记
 * @param safetyNotices 高风险时返回的固定安全提示
 */
public record RepairTicketCreatePayload(String labId, String labName, String equipmentInfo, String description,
                                       boolean safetyRisk, List<String> safetyNotices) {

    /**
     * 防止调用方在草案缓存后修改安全提示集合。
     */
    public RepairTicketCreatePayload {
        safetyNotices = safetyNotices == null ? List.of() : List.copyOf(safetyNotices);
    }
}
