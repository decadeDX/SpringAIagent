package io.github.decadedx.springaiagent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.decadedx.springaiagent.enums.ActionExecutionStatus;
import io.github.decadedx.springaiagent.enums.ActionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户确认动作的持久化幂等依据；成功记录在 Redis 草案过期后仍可安全重放。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("action_execution")
public class ActionExecution {

    /** 服务端随机生成且全局唯一的动作标识。 */
    @TableId("action_id")
    private String actionId;

    /** 草案所属的认证用户。 */
    @TableField("user_id")
    private Long userId;

    /** 创建草案的聊天或业务会话。 */
    @TableField("session_id")
    private String sessionId;

    /** 最终执行的领域动作。 */
    @TableField("action_type")
    private ActionType actionType;

    /** 当前执行状态。 */
    @TableField("execution_status")
    private ActionExecutionStatus executionStatus;

    /** 确认时使用的可信草案 JSON。 */
    private String payload;

    /** 成功创建或变更的预约、工单主键。 */
    @TableField("business_id")
    private Long businessId;

    /** 可安全返回给原用户的 JSON 结果摘要。 */
    @TableField("result_summary")
    private String resultSummary;

    /** 领域失败时保存的稳定内部错误码。 */
    @TableField("failure_code")
    private String failureCode;

    /** 记录创建时间，数据库以 UTC 保存。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 最近一次状态变化时间，数据库以 UTC 保存。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
