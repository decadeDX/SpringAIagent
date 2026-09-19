package io.github.decadedx.springaiagent.enums;

/**
 * 学生安全培训的审核结果，用于后续受限实验室预约校验。
 */
public enum TrainingStatus {
    /** 尚未完成或尚未审核培训。 */
    PENDING,
    /** 已通过安全培训。 */
    PASSED,
    /** 未通过安全培训。 */
    FAILED
}
