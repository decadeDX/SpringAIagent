package io.github.decadedx.springaiagent.vo;

import java.time.OffsetDateTime;

/**
 * 登录成功后的令牌及用户摘要；访问令牌仅在该响应中短暂返回。
 *
 * @param accessToken Bearer 访问令牌
 * @param tokenType 固定为 Bearer
 * @param expiresAt 令牌到期时间，使用带时区偏移的 ISO-8601 表示
 * @param user 已脱敏的当前用户摘要
 */
public record LoginVO(String accessToken, String tokenType, OffsetDateTime expiresAt, UserSummaryVO user) {
}
