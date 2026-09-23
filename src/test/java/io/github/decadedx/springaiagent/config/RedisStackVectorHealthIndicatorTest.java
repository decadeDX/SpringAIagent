package io.github.decadedx.springaiagent.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 Redis Stack 索引健康检查不会将索引访问异常误报为可用。
 */
class RedisStackVectorHealthIndicatorTest {

    /** FT.INFO 返回索引信息时报告可用。 */
    @Test
    void reportsUpWhenIndexIsAccessible() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(ArgumentMatchers.<RedisCallback<Object>>any())).thenReturn(new Object());

        assertThat(new RedisStackVectorHealthIndicator(redisTemplate, "lab-knowledge-index")
                .health().getStatus().getCode()).isEqualTo("UP");
    }

    /** FT.INFO 失败时只报告 DOWN，不暴露底层异常。 */
    @Test
    void reportsDownWhenIndexCommandFails() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(ArgumentMatchers.<RedisCallback<Object>>any()))
                .thenThrow(new IllegalStateException("Redis address must stay private"));

        assertThat(new RedisStackVectorHealthIndicator(redisTemplate, "lab-knowledge-index")
                .health().getStatus().getCode()).isEqualTo("DOWN");
    }
}
