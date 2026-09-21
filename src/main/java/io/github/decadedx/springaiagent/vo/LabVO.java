package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.decadedx.springaiagent.enums.LabStatus;

import java.time.LocalTime;

/**
 * 面向客户端展示的实验室摘要，不包含管理端版本号和内部审计字段。
 *
 * @param id 实验室业务编号
 * @param name 实验室名称
 * @param capacity 最大容纳人数
 * @param equipmentDescription 设备说明
 * @param openTime 上海当地开放时间
 * @param closeTime 上海当地关闭时间
 * @param status 当前运行状态
 */
public record LabVO(String id, String name, int capacity, String equipmentDescription,
                    @JsonFormat(pattern = "HH:mm") LocalTime openTime,
                    @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
                    LabStatus status) {
}
