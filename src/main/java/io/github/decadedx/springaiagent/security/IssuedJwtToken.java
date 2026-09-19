package io.github.decadedx.springaiagent.security;

import java.time.Instant;

/**
 * 认证服务签发访问令牌后使用的内部结果，避免重复解析令牌取得到期时间。
 *
 * @param value JWT 紧凑序列化文本
 * @param expiresAt 到期时刻
 */
public record IssuedJwtToken(String value, Instant expiresAt) {
}
