package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 预约占用的一个整点时隙；唯一键是跨用户并发抢占时的最终一致性保障。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("reservation_slot")
public class ReservationSlot {

    /** 时隙记录主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 被占用的实验室业务编号。 */
    @TableField("lab_id")
    private String labId;

    /** 被占用整点的开始时刻，数据库以 UTC 保存。 */
    @TableField("slot_start_time")
    private LocalDateTime slotStartTime;

    /** 对应预约聚合根的数据库主键。 */
    @TableField("reservation_id")
    private Long reservationId;

    /** 时隙写入时刻，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
