package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.ActionDraftService;
import io.github.decadedx.springaiagent.service.StoredActionDraft;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 使用 Redis 保存五分钟确认草案，并以用户和会话隔离可执行动作。
 */
@Service
public class ActionDraftServiceImpl implements ActionDraftService {

    /** 草案序列化格式版本。 */
    private static final int SCHEMA_VERSION = 1;

    /** 草案固定有效期。 */
    private static final Duration TTL = Duration.ofMinutes(5);

    /** Redis 动作草案键前缀。 */
    private static final String ACTION_KEY_PREFIX = "agent:action:";

    /** 同一会话同类动作的最新草案索引前缀。 */
    private static final String LATEST_KEY_PREFIX = "agent:action:latest:";

    /** Redis 字符串访问入口。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 草案 JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /** 可替换业务时钟。 */
    private final Clock clock;

    /**
     * 创建草案缓存服务。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     * @param clock 业务时钟
     */
    public ActionDraftServiceImpl(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /** {@inheritDoc} */
    @Override
    public ActionDraftVO create(ActionType actionType, String sessionId, Object payload, List<String> notices) {
        Long userId = CurrentUser.requireId();
        String effectiveSessionId = sessionId == null || sessionId.isBlank() ? "web_" + UUID.randomUUID() : sessionId;
        String actionId = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).plus(TTL);
        JsonNode payloadNode = objectMapper.valueToTree(payload);
        StoredActionDraft draft = new StoredActionDraft(SCHEMA_VERSION, actionId, userId, effectiveSessionId,
                actionType, payloadNode, notices == null ? List.of() : notices, expiresAt);
        try {
            String latestKey = latestKey(userId, effectiveSessionId, actionType);
            String previousActionId = stringRedisTemplate.opsForValue().get(latestKey);
            if (previousActionId != null) {
                stringRedisTemplate.delete(actionKey(previousActionId));
            }
            stringRedisTemplate.opsForValue().set(actionKey(actionId), objectMapper.writeValueAsString(draft), TTL);
            stringRedisTemplate.opsForValue().set(latestKey, actionId, TTL);
        } catch (Exception exception) {
            throw redisUnavailable();
        }
        Map<String, Object> responsePayload = objectMapper.convertValue(payloadNode, Map.class);
        return new ActionDraftVO(actionId, actionType, effectiveSessionId, expiresAt, responsePayload, draft.notices());
    }

    /** {@inheritDoc} */
    @Override
    public StoredActionDraft find(String actionId) {
        try {
            String content = stringRedisTemplate.opsForValue().get(actionKey(actionId));
            if (content == null) {
                return null;
            }
            StoredActionDraft draft = objectMapper.readValue(content, StoredActionDraft.class);
            if (draft.schemaVersion() != SCHEMA_VERSION || draft.expiresAt().isBefore(OffsetDateTime.now(clock))) {
                delete(actionId);
                return null;
            }
            return draft;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String actionId) {
        try {
            stringRedisTemplate.delete(actionKey(actionId));
        } catch (Exception exception) {
            throw redisUnavailable();
        }
    }

    /**
     * 生成单个草案 Redis 键。
     *
     * @param actionId 动作标识
     * @return Redis 键
     */
    private String actionKey(String actionId) {
        return ACTION_KEY_PREFIX + actionId;
    }

    /**
     * 生成同会话同类型草案的反向索引键。
     *
     * @param userId 当前用户
     * @param sessionId 当前会话
     * @param actionType 动作类型
     * @return Redis 键
     */
    private String latestKey(Long userId, String sessionId, ActionType actionType) {
        return LATEST_KEY_PREFIX + userId + ":" + sessionId + ":" + actionType;
    }

    /**
     * 统一返回不能安全创建或读取草案时的依赖错误。
     *
     * @return Redis 不可用业务异常
     */
    private BusinessException redisUnavailable() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.REDIS_UNAVAILABLE,
                "草案服务暂不可用，请稍后重试");
    }
}
