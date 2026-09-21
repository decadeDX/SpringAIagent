package io.github.decadedx.springaiagent.enums;

/**
 * 报修工单的受控处理状态；只允许按既定顺序向后流转。
 */
public enum RepairTicketStatus {
    /** 学生已确认提交，等待管理员受理。 */
    SUBMITTED,
    /** 管理员正在处理，必须已记录处理说明。 */
    PROCESSING,
    /** 管理员已完成处理，必须已记录最终处理说明。 */
    RESOLVED
}
