package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.vo.LabVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存可丢失的实验室详情；所有 Redis 故障都由调用方安全回源 MySQL。
 */
@Component
public class LabDetailCache {

    /** 实验室详情键前缀。 */
    private static final String KEY_PREFIX = "lab:detail:";

    /** 实验室详情缓存有效期。 */
    private static final Duration TTL = Duration.ofMinutes(10);

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 详情 JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * 创建实验室详情缓存。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     */
    public LabDetailCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 尝试读取详情缓存，Redis 不可用或缓存损坏时视为未命中。
     *
     * @param labId 实验室业务编号
     * @return 命中的详情
     */
    public Optional<LabVO> get(String labId) {
        try {
            String content = stringRedisTemplate.opsForValue().get(key(labId));
            if (content == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(content, LabVO.class));
        } catch (Exception exception) {
            deleteQuietly(labId);
            return Optional.empty();
        }
    }

    /**
     * 写入详情缓存；缓存写入失败不影响 MySQL 查询结果。
     *
     * @param detail 对外详情
     */
    public void put(LabVO detail) {
        try {
            stringRedisTemplate.opsForValue().set(key(detail.id()), objectMapper.writeValueAsString(detail), TTL);
        } catch (Exception exception) {
        }
    }

    /**
     * 删除指定实验室缓存；数据库已提交后的失效失败仅等待旧 TTL 到期。
     *
     * @param labId 实验室业务编号
     */
    public void evict(String labId) {
        deleteQuietly(labId);
    }

    /**
     * 构造 Redis 键。
     *
     * @param labId 实验室业务编号
     * @return Redis 键
     */
    private String key(String labId) {
        return KEY_PREFIX + labId;
    }

    /**
     * 尽力删除缓存，避免缓存故障影响主业务。
     *
     * @param labId 实验室业务编号
     */
    private void deleteQuietly(String labId) {
        try {
            stringRedisTemplate.delete(key(labId));
        } catch (Exception exception) {
        }
    }
}
