package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.common.ApplicationMetrics;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.ChatRateLimiter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 使用 Lua 原子递增并设置 TTL 的 Redis 聊天限流器。
 */
@Service
public class RedisChatRateLimiter implements ChatRateLimiter {

    /** 每个用户每自然分钟允许的聊天次数。 */
    private static final long LIMIT = 20L;

    /** 固定窗口保留秒数。 */
    private static final String WINDOW_TTL_SECONDS = "60";

    /** Redis 限流键前缀。 */
    private static final String KEY_PREFIX = "rate:chat:";

    /** 首次计数时原子设置 TTL 的 Lua 脚本。 */
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; return count;", Long.class);

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 业务时钟。 */
    private final Clock clock;

    /** 聊天限流指标。 */
    private final ApplicationMetrics applicationMetrics;

    /**
     * 创建 Redis 聊天限流器。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param clock 业务时钟
     * @param applicationMetrics 聊天限流指标
     */
    public RedisChatRateLimiter(StringRedisTemplate stringRedisTemplate, Clock clock,
                                ApplicationMetrics applicationMetrics) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
        this.applicationMetrics = applicationMetrics;
    }

    /** {@inheritDoc} */
    @Override
    public void check() {
        Long userId = CurrentUser.requireId();
        long window = Instant.now(clock).getEpochSecond() / 60L;
        try {
            Long count = stringRedisTemplate.execute(INCREMENT_SCRIPT, List.of(key(userId, window)), WINDOW_TTL_SECONDS);
            if (count == null) {
                throw redisUnavailable();
            }
            if (count > LIMIT) {
                applicationMetrics.chatRateLimited();
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, ApiCode.RATE_LIMITED,
                        "聊天请求过于频繁，请稍后再试");
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /**
     * 构造用户与自然分钟隔离的 Redis 键。
     *
     * @param userId 当前用户
     * @param window 当前 epoch 分钟
     * @return Redis 键
     */
    private String key(Long userId, long window) {
        return KEY_PREFIX + userId + ":" + window;
    }

    /**
     * 返回 Redis 不可用时的保守聊天拒绝错误。
     *
     * @return 依赖错误
     */
    private BusinessException redisUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.REDIS_UNAVAILABLE,
                "聊天服务暂不可用，请稍后重试");
    }
}
