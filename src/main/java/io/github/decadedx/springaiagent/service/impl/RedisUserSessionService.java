package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.service.UserSessionService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 使用 Redis 原子键实现每个用户只保留一个活动会话。
 */
@Service
public class RedisUserSessionService implements UserSessionService {

    /** 活动会话 Redis 键的固定前缀。 */
    private static final String KEY_PREFIX = "auth:session:";

    /** 仅当值匹配时删除会话键的原子 Lua 脚本。 */
    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]); end; return 0;",
            Long.class);

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 创建用户会话服务。
     *
     * @param stringRedisTemplate Redis 字符串访问入口
     */
    public RedisUserSessionService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public boolean reserve(Long userId, String sessionId, Instant expiresAt) {
        try {
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            Boolean reserved = stringRedisTemplate.opsForValue().setIfAbsent(key(userId), sessionId, ttl);
            return Boolean.TRUE.equals(reserved);
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isActive(Long userId, String sessionId) {
        try {
            return sessionId.equals(stringRedisTemplate.opsForValue().get(key(userId)));
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void release(Long userId, String sessionId) {
        try {
            stringRedisTemplate.execute(DELETE_IF_MATCHES_SCRIPT, List.of(key(userId)), sessionId);
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /**
     * 构造单个用户的活动会话键。
     *
     * @param userId 用户主键
     * @return Redis 会话键
     */
    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    /**
     * 将 Redis 访问异常转换为不允许绕过的认证依赖错误。
     *
     * @return 统一 Redis 不可用异常
     */
    private BusinessException redisUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.REDIS_UNAVAILABLE,
                "登录服务暂不可用，请稍后重试");
    }
}
