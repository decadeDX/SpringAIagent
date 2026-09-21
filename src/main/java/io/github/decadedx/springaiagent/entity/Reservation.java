package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.decadedx.springaiagent.enums.ReservationStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 一次整间实验室预约的聚合根；创建和取消必须与其时隙记录位于同一事务。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("reservation")
public class Reservation {

    /** 数据库预约主键，用于生成对外展示的预约编号。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 预约所属用户，仅能从认证上下文取得。 */
    @TableField("user_id")
    private Long userId;

    /** 被预约的实验室业务编号。 */
    @TableField("lab_id")
    private String labId;

    /** 预约开始时刻，数据库以 UTC 保存。 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 预约结束时刻，数据库以 UTC 保存且不包含该时刻对应时隙。 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 本次预约的实际参与人数。 */
    @TableField("participant_count")
    private Integer participantCount;

    /** 已确认或已取消的预约状态。 */
    private ReservationStatus status;

    /** 取消操作完成时写入的 UTC 时刻；有效预约必须为空。 */
    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    /** 预约事实创建时刻，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 预约事实最近更新时刻，数据库以 UTC 保存。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 查询本人预约时由关联查询填充的实验室名称，不参与持久化。 */
    @TableField(exist = false)
    private String labName;
}
