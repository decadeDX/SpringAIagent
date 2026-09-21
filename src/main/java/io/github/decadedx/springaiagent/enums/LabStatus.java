package io.github.decadedx.springaiagent.enums;

/**
 * 实验室当前可服务状态；只有 {@link #ACTIVE} 可以接受新的预约。
 */
public enum LabStatus {
    /** 正常开放，可查询并接受预约。 */
    ACTIVE,
    /** 临时维护，保留历史预约但拒绝新预约。 */
    MAINTENANCE,
    /** 已停用，不再对外提供预约服务。 */
    DISABLED
}
