package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.ChatSessionService;
import io.github.decadedx.springaiagent.vo.ChatSessionVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Redis 的用户隔离聊天上下文，任何读取和写入都会校验认证用户。
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    /** 会话序列化格式版本。 */
    private static final int SCHEMA_VERSION = 1;

    /** 会话滑动过期时长。 */
    private static final Duration TTL = Duration.ofMinutes(30);

    /** 保存六组问答的最大消息数。 */
    private static final int MAX_MESSAGES = 12;

    /** Redis 会话键前缀。 */
    private static final String KEY_PREFIX = "chat:session:";

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 业务时钟。 */
    private final Clock clock;

    /**
     * 创建聊天会话服务。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     * @param clock 业务时钟
     */
    public ChatSessionServiceImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public ChatSessionVO create(String name) {
        Long userId = CurrentUser.requireId();
        String sessionId = "chat_" + UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now(clock);
        StoredChatSession session = new StoredChatSession(SCHEMA_VERSION, userId, sessionId, name, createdAt, List.of());
        write(session);
        return new ChatSessionVO(sessionId, name, createdAt);
    }

    /** {@inheritDoc} */
    @Override
    public StoredChatSession require(String sessionId) {
        Long userId = CurrentUser.requireId();
        try {
            String content = stringRedisTemplate.opsForValue().get(key(userId, sessionId));
            if (content == null) {
                throw notFound();
            }
            StoredChatSession session = objectMapper.readValue(content, StoredChatSession.class);
            if (session.schemaVersion() != SCHEMA_VERSION || !userId.equals(session.userId())
                    || !sessionId.equals(session.sessionId())) {
                throw notFound();
            }
            stringRedisTemplate.expire(key(userId, sessionId), TTL);
            return session;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void append(String sessionId, String role, String content) {
        StoredChatSession session = require(sessionId);
        List<ChatEntry> messages = new ArrayList<>(session.messages());
        messages.add(new ChatEntry(role, content));
        if (messages.size() > MAX_MESSAGES) {
            messages = new ArrayList<>(messages.subList(messages.size() - MAX_MESSAGES, messages.size()));
        }
        write(new StoredChatSession(SCHEMA_VERSION, session.userId(), session.sessionId(), session.name(),
                session.createdAt(), messages));
    }

    /**
     * 持久化并续期会话。
     *
     * @param session 当前会话
     */
    private void write(StoredChatSession session) {
        try {
            stringRedisTemplate.opsForValue().set(key(session.userId(), session.sessionId()),
                    objectMapper.writeValueAsString(session), TTL);
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /**
     * 构造用户隔离的 Redis 键。
     *
     * @param userId 当前用户
     * @param sessionId 会话标识
     * @return Redis 键
     */
    private String key(Long userId, String sessionId) {
        return KEY_PREFIX + userId + ":" + sessionId;
    }

    /**
     * 返回不泄露他人会话是否存在的错误。
     *
     * @return 不存在异常
     */
    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, ApiCode.NOT_FOUND, "会话不存在或已过期");
    }

    /**
     * 返回 Redis 不可用错误。
     *
     * @return 依赖错误
     */
    private BusinessException redisUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.REDIS_UNAVAILABLE,
                "会话服务暂不可用，请稍后重试");
    }
}
