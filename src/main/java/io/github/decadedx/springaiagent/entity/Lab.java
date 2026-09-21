package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.decadedx.springaiagent.enums.LabStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 实验室基础资料；开放时段和状态是预约确认时必须重新读取的业务事实。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("lab")
public class Lab {

    /** 面向业务使用的实验室编号，例如 LAB-B402。 */
    @TableId
    private String id;

    /** 用于筛选和展示的实验室名称。 */
    private String name;

    /** 整间实验室可容纳的最大参与人数。 */
    private Integer capacity;

    /** 可按关键词筛选的设备与环境说明。 */
    private String equipmentDescription;

    /** 以 Asia/Shanghai 当地时间表示的每日开放时间。 */
    private LocalTime openTime;

    /** 以 Asia/Shanghai 当地时间表示的每日关闭时间。 */
    private LocalTime closeTime;

    /** 决定是否允许创建新预约的运行状态。 */
    private LabStatus status;

    /** 管理端更新时使用的乐观锁版本。 */
    private Long version;

    /** 资料首次创建时刻，数据库以 UTC 保存。 */
    private LocalDateTime createdAt;

    /** 资料最近更新时间，数据库以 UTC 保存。 */
    private LocalDateTime updatedAt;
}
