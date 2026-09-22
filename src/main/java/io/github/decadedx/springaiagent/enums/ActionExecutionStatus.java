package io.github.decadedx.springaiagent.enums;

/**
 * 动作确认在持久化幂等记录中的执行状态。
 */
public enum ActionExecutionStatus {
    EXECUTING,
    SUCCEEDED,
    FAILED
}
