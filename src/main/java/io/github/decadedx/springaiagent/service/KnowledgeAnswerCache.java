package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.config.KnowledgeProperties;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * 缓存不含用户实时数据的独立 RAG 结果；缓存键始终绑定已发布知识版本号。
 */
@Component
public class KnowledgeAnswerCache {

    /** Redis 中保存已发布知识版本计数的固定键。 */
    private static final String VERSION_KEY = "knowledge:published-version";

    /** RAG 回答缓存键前缀。 */
    private static final String ANSWER_KEY_PREFIX = "knowledge:answer:";

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 统一 JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 缓存有效期与存储相关配置。 */
    private final KnowledgeProperties knowledgeProperties;

    /**
     * 创建知识问答缓存服务。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     * @param knowledgeProperties 缓存有效期配置
     */
    public KnowledgeAnswerCache(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
                                KnowledgeProperties knowledgeProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.knowledgeProperties = knowledgeProperties;
    }

    /**
     * 读取当前发布版本和问题对应的缓存答案。
     *
     * @param question 用户原始问题
     * @return 命中时的完整问答响应
     */
    public Optional<RagAnswerVO> get(String question) {
        String content = stringRedisTemplate.opsForValue().get(answerKey(question));
        if (content == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(content, RagAnswerVO.class));
        } catch (JacksonException exception) {
            stringRedisTemplate.delete(answerKey(question));
            return Optional.empty();
        }
    }

    /**
     * 缓存一次仅基于公开知识资料生成的问答结果。
     *
     * @param question 用户原始问题
     * @param answer 待缓存的问答响应
     */
    public void put(String question, RagAnswerVO answer) {
        try {
            stringRedisTemplate.opsForValue().set(answerKey(question), objectMapper.writeValueAsString(answer),
                    knowledgeProperties.cacheTtl());
        } catch (JacksonException exception) {
            throw new IllegalStateException("知识问答缓存序列化失败", exception);
        }
    }

    /**
     * 增加全局已发布知识版本，令所有旧缓存键即时失效。
     */
    public void bumpPublishedVersion() {
        stringRedisTemplate.opsForValue().increment(VERSION_KEY);
    }

    /**
     * 组合当前发布版本与问题摘要，生成不会泄露问题正文的缓存键。
     *
     * @param question 用户原始问题
     * @return 当前版本下唯一的缓存键
     */
    private String answerKey(String question) {
        String version = stringRedisTemplate.opsForValue().get(VERSION_KEY);
        return ANSWER_KEY_PREFIX + (version == null ? "0" : version) + ":" + sha256(question.trim());
    }

    /**
     * 计算问题文本的 SHA-256 十六进制摘要。
     *
     * @param value 待计算文本
     * @return 小写十六进制摘要
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }
}
