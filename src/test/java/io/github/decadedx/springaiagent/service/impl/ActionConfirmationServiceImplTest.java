package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApplicationMetrics;
import io.github.decadedx.springaiagent.dto.ActionConfirmDTO;
import io.github.decadedx.springaiagent.entity.ActionExecution;
import io.github.decadedx.springaiagent.enums.ActionExecutionStatus;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.mapper.ActionExecutionMapper;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.service.ActionDraftService;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证动作确认结果在首次执行和幂等重放时遵守相同的公开主键类型。
 */
class ActionConfirmationServiceImplTest {

    /** 清除测试认证信息，避免影响其他用例。 */
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 旧记录的数值主键重放时必须转换为字符串，不要求迁移历史 JSON。
     */
    @Test
    void shouldNormalizeLegacyNumericIdsWhenReplaying() {
        ActionExecutionMapper mapper = mock(ActionExecutionMapper.class);
        ActionExecution execution = new ActionExecution();
        execution.setActionId("action-01");
        execution.setUserId(1L);
        execution.setActionType(ActionType.CREATE_REPAIR_TICKET);
        execution.setExecutionStatus(ActionExecutionStatus.SUCCEEDED);
        execution.setResultSummary("{\"businessId\":2102688050821632001,\"ticketId\":2102688050821632001,"
                + "\"ticketNo\":\"RPT-2102688050821632001\",\"status\":\"SUBMITTED\"}");
        when(mapper.selectById("action-01")).thenReturn(execution);
        authenticateStudent();

        ActionConfirmationServiceImpl service = new ActionConfirmationServiceImpl(mapper, mock(SysUserMapper.class),
                mock(ActionDraftService.class),
                mock(ReservationService.class), mock(RepairTicketService.class), new ObjectMapper(),
                mock(PlatformTransactionManager.class), mock(ApplicationMetrics.class));

        var result = service.confirm("action-01", new ActionConfirmDTO("web-01"));

        assertThat(result.result()).containsEntry("businessId", "2102688050821632001")
                .containsEntry("ticketId", "2102688050821632001");
    }

    /** 设置与重放记录匹配的当前学生身份。 */
    private void authenticateStudent() {
        AuthenticatedUser user = new AuthenticatedUser(1L, io.github.decadedx.springaiagent.enums.UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }
}
