package io.github.decadedx.springaiagent.enums;

/**
 * 系统用户的已验证角色；角色值同时用于 JWT Claim 与 Spring Security 授权。
 */
public enum UserRole {
    /** 可查询和办理本人实验室业务的学生。 */
    STUDENT,
    /** 可处理实验室、工单和知识库管理操作的管理员。 */
    ADMIN
}
