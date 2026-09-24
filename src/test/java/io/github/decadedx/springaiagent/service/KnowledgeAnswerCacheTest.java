package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.config.KnowledgeProperties;
import io.github.decadedx.springaiagent.vo.KnowledgeCitationVO;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;
import io.github.decadedx.springaiagent.vo.RagRetrievalVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeAnswerCacheTest {

    @Test
    void shouldDiscardLegacyRefusalCacheEntry() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(new ObjectMapper().writeValueAsString(refusal()));
        KnowledgeAnswerCache cache = cache(redisTemplate);

        assertThat(cache.get("GPU 实验室有哪些限制？")).isEmpty();

        verify(redisTemplate).delete(anyString());
    }

    @Test
    void shouldNotCacheRefusal() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        KnowledgeAnswerCache cache = cache(redisTemplate);

        cache.put("GPU 实验室有哪些限制？", refusal());

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldCacheAnswerWithCitation() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        KnowledgeAnswerCache cache = cache(redisTemplate);

        cache.put("GPU 实验室有哪些限制？", answerWithCitation());

        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofMinutes(10)));
    }

    private KnowledgeAnswerCache cache(StringRedisTemplate redisTemplate) {
        return new KnowledgeAnswerCache(redisTemplate, new ObjectMapper(),
                new KnowledgeProperties(false, Path.of("knowledge/uploads"), Duration.ofMinutes(10)));
    }

    private RagAnswerVO refusal() {
        return new RagAnswerVO("当前知识库中没有足够信息", List.of(), new RagRetrievalVO(5, 0, 0));
    }

    private RagAnswerVO answerWithCitation() {
        return new RagAnswerVO("预约人必须完成安全培训。",
                List.of(new KnowledgeCitationVO("人工智能实验室使用指南", "v1.0", "2-1", "完成安全培训")),
                new RagRetrievalVO(5, 1, 0));
    }
}
