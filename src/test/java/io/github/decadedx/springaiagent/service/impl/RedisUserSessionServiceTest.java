package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证单账号 Redis 会话在基础设施异常时不会被放行。
 */
class RedisUserSessionServiceTest {

    /**
     * Redis 不可用时，登录会话登记必须返回统一的依赖错误。
     */
    @Test
    void rejectsSessionReservationWhenRedisIsUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        RedisUserSessionService service = new RedisUserSessionService(redisTemplate);

        assertThatThrownBy(() -> service.reserve(1L, "session-1", Instant.now().plusSeconds(60)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode())
                .isEqualTo(ApiCode.REDIS_UNAVAILABLE);
    }
}
