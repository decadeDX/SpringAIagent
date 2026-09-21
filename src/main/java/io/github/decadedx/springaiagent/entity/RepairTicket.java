package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 学生确认提交的设备报修工单；工单状态不会自动改变实验室运行状态。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("repair_ticket")
public class RepairTicket {

    /** 工单数据库主键，用于生成对外展示编号。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 提交报修的学生主键，仅从认证上下文写入。 */
    @TableField("user_id")
    private Long userId;

    /** 故障所属实验室业务编号。 */
    @TableField("lab_id")
    private String labId;

    /** 学生填写的设备名称或资产编号。 */
    @TableField("equipment_info")
    private String equipmentInfo;

    /** 学生填写的故障现象说明。 */
    private String description;

    /** 是否包含需立即停用设备的高风险信号。 */
    @TableField("safety_risk")
    private Boolean safetyRisk;

    /** 当前工单所处的受控处理状态。 */
    private RepairTicketStatus status;

    /** 每次管理员流转时必须写入的处理说明。 */
    @TableField("resolution_note")
    private String resolutionNote;

    /** 最近一次处理该工单的管理员主键。 */
    @TableField("processed_by")
    private Long processedBy;

    /** 管理员流转时比较的乐观锁版本号。 */
    @Version
    private Long version;

    /** 工单确认提交时刻，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 工单最近处理时刻，数据库以 UTC 保存。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
