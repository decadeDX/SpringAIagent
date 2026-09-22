package io.github.decadedx.springaiagent.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 在启用 RAG 时验证 Redis Stack 向量索引可访问，避免普通 PING 掩盖搜索模块缺失。
 */
@Component("redisStackVector")
@ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
public class RedisStackVectorHealthIndicator implements HealthIndicator {

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 当前 RAG 向量索引名称。 */
    private final String indexName;

    /**
     * 创建 Redis Stack 索引健康检查。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param indexName 向量索引名称
     */
    public RedisStackVectorHealthIndicator(StringRedisTemplate stringRedisTemplate,
                                           @org.springframework.beans.factory.annotation.Value("${spring.ai.vectorstore.redis.index-name:lab-knowledge-index}")
                                           String indexName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.indexName = indexName;
    }

    /**
     * 执行 Redis Stack 的 FT.INFO 命令；不向健康响应输出索引名或 Redis 地址。
     *
     * @return 可用性状态
     */
    @Override
    public Health health() {
        try {
            Object response = stringRedisTemplate.execute((RedisCallback<Object>) connection -> connection.execute("FT.INFO",
                    indexName.getBytes(StandardCharsets.UTF_8)));
            return response == null ? Health.down().build() : Health.up().build();
        } catch (Exception exception) {
            return Health.down().build();
        }
    }
}
