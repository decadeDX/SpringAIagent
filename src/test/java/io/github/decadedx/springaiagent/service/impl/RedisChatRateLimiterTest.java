package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 Redis 聊天限流的边界与保守故障策略。
 */
class RedisChatRateLimiterTest {

    /** 每个用例后清除认证上下文。 */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 第二十一条消息必须被限流器拒绝。
     */
    @Test
    void rejectsTheTwentyFirstMessageInOneWindow() {
        authenticateStudent();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<List<String>>any(),
                ArgumentMatchers.<Object>any())).thenReturn(21L);
        RedisChatRateLimiter limiter = new RedisChatRateLimiter(redisTemplate,
                Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(limiter::check)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(ApiCode.RATE_LIMITED);
    }

    /**
     * Redis 调用失败时不得放行聊天请求。
     */
    @Test
    void rejectsChatWhenRedisIsUnavailable() {
        authenticateStudent();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        doThrow(new IllegalStateException("redis unavailable")).when(redisTemplate)
                .execute(ArgumentMatchers.<RedisScript<Long>>any(), ArgumentMatchers.<List<String>>any(),
                        ArgumentMatchers.<Object>any());
        RedisChatRateLimiter limiter = new RedisChatRateLimiter(redisTemplate,
                Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(limiter::check)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(ApiCode.REDIS_UNAVAILABLE);
    }

    /**
     * 在安全上下文写入当前认证学生。
     */
    private void authenticateStudent() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(1L, UserRole.STUDENT), null, List.of()));
    }
}
