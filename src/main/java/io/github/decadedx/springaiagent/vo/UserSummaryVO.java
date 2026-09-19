package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;

/**
 * 登录成功后可返回前端的用户摘要，不包含密码摘要或任何凭据。
 *
 * @param id 用户主键
 * @param username 用户账号名
 * @param role 已验证角色
 * @param trainingStatus 培训审核结果
 */
public record UserSummaryVO(Long id, String username, UserRole role, TrainingStatus trainingStatus) {
}
