package io.github.decadedx.springaiagent.service;

import java.time.Instant;

/**
 * 管理每个用户唯一的 Redis 活动会话，防止同一账号并发登录。
 */
public interface UserSessionService {

    /**
     * 原子登记用户会话；已有未过期会话时不覆盖。
     *
     * @param userId 用户主键
     * @param sessionId 待登记的随机会话标识
     * @param expiresAt 会话与 JWT 共同的失效时刻
     * @return 成功占用会话位置时为 {@code true}
     */
    boolean reserve(Long userId, String sessionId, Instant expiresAt);

    /**
     * 判断请求携带的会话是否仍是该用户的唯一活动会话。
     *
     * @param userId 用户主键
     * @param sessionId JWT 中的随机会话标识
     * @return Redis 保存的当前会话与传入标识一致时为 {@code true}
     */
    boolean isActive(Long userId, String sessionId);

    /**
     * 仅在 Redis 当前值与请求会话匹配时释放会话，避免旧令牌误删新会话。
     *
     * @param userId 用户主键
     * @param sessionId 当前 JWT 的随机会话标识
     */
    void release(Long userId, String sessionId);
}
