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
 * 单次 Agent 工具调用的脱敏审计记录，不保存模型思维链或密钥。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("agent_trace")
public class AgentTrace {

    /** 审计记录主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** HTTP 请求追踪标识。 */
    @TableField("request_id")
    private String requestId;

    /** 可空的聊天会话标识。 */
    @TableField("session_id")
    private String sessionId;

    /** 发起调用的认证用户。 */
    @TableField("user_id")
    private Long userId;

    /** 已注册工具名称。 */
    @TableField("tool_name")
    private String toolName;

    /** 过滤敏感字段后的参数 JSON。 */
    @TableField("redacted_arguments")
    private String redactedArguments;

    /** 不含个人数据和密钥的结果摘要。 */
    @TableField("result_summary")
    private String resultSummary;

    /** 工具调用耗时毫秒。 */
    @TableField("duration_ms")
    private int durationMs;

    /** 工具失败时的稳定错误码。 */
    @TableField("error_code")
    private String errorCode;

    /** 记录写入时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
