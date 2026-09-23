package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.dto.ChatMessageDTO;
import io.github.decadedx.springaiagent.service.AgentCallContext;
import io.github.decadedx.springaiagent.service.AgentModelClient;
import io.github.decadedx.springaiagent.service.ChatSessionService;
import io.github.decadedx.springaiagent.service.LabAssistantTools;
import io.github.decadedx.springaiagent.service.RelativeDateResolver;
import io.github.decadedx.springaiagent.vo.ChatMessageVO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证没有真实工具调用时，模型不能伪造查询失败。
 */
class AgentOrchestrationServiceImplTest {

    /**
     * 模型未查询数据库却声称数据库错误时，响应必须改为中性提示。
     */
    @Test
    void shouldReplaceUngroundedDatabaseFailureWithoutToolCall() {
        ChatSessionService chatSessionService = mock(ChatSessionService.class);
        when(chatSessionService.require("chat-1")).thenReturn(new ChatSessionService.StoredChatSession(1, 1L,
                "chat-1", null, OffsetDateTime.now(), List.of()));
        AgentModelClient agentModelClient = (systemPrompt, history, message, tools) -> "查询候选实验室时系统出现数据库错误";
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T00:00:00Z"), ZoneOffset.UTC);
        AgentOrchestrationServiceImpl service = new AgentOrchestrationServiceImpl(chatSessionService, agentModelClient,
                mock(LabAssistantTools.class), new AgentCallContext(), clock, new RelativeDateResolver(clock));

        ChatMessageVO response = service.send("chat-1", new ChatMessageDTO("查询软件工程实验室可用时段"));

        assertThat(response.answer()).isEqualTo("我还没有获得可验证的实时查询结果。请提供具体的开始和结束时间，我会据此查询。");
        assertThat(response.toolCallCount()).isZero();
    }
}
