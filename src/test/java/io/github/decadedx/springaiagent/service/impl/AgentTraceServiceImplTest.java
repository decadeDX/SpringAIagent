package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.SensitiveDataSanitizer;
import io.github.decadedx.springaiagent.entity.AgentTrace;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.mapper.AgentTraceMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 验证 Agent 审计记录符合数据库 JSON 字段的存储约束，且不会保留原始工具参数。
 */
class AgentTraceServiceImplTest {

    /** 清理测试设置的认证上下文，避免影响其他测试。 */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 脱敏后的工具参数仍必须是合法 JSON，才能写入 agent_trace.redacted_arguments。
     */
    @Test
    void shouldPersistValidJsonForRedactedArguments() {
        AgentTraceMapper mapper = mock(AgentTraceMapper.class);
        AgentTraceServiceImpl service = new AgentTraceServiceImpl(mapper, new SensitiveDataSanitizer());
        authenticate();

        service.record("session-1", "searchKnowledge", "{\"question\":\"已脱敏\"}", "RagAnswerVO", 12, null);

        ArgumentCaptor<AgentTrace> trace = ArgumentCaptor.forClass(AgentTrace.class);
        verify(mapper).insert(trace.capture());
        assertThat(trace.getValue().getRedactedArguments()).isEqualTo("{\"redacted\":true}");
    }

    /** 设置最小认证上下文，使审计服务能取得当前用户编号。 */
    private void authenticate() {
        AuthenticatedUser user = new AuthenticatedUser(1L, UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }
}
