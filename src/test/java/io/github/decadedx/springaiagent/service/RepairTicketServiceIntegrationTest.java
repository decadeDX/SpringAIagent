package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.dto.RepairDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketCreatePayload;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketUpdateDTO;
import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.vo.RepairTicketVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepairTicketServiceIntegrationTest {

    @Autowired
    private RepairTicketService repairTicketService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM repair_ticket");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM repair_ticket");
    }

    @Test
    void shouldRejectMissingRequiredFieldsAndUnknownLab() {
        authenticate(1L, UserRole.STUDENT);

        assertThatThrownBy(() -> repairTicketService.prepareCreate(new RepairDraftCreateDTO("LAB-A301", "", "显示器无信号", false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.REPAIR_REQUIRED_FIELD_MISSING));
        assertThatThrownBy(() -> repairTicketService.prepareCreate(new RepairDraftCreateDTO("LAB-UNKNOWN", "GPU-03", "无法启动", false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.LAB_NOT_FOUND));
    }

    @Test
    void shouldForceHighRiskAndKeepLabStatusUnchangedWhenConfirming() {
        authenticate(1L, UserRole.STUDENT);

        RepairTicketCreatePayload payload = repairTicketService.prepareCreate(
                new RepairDraftCreateDTO("LAB-B402", "GPU-03", "开机后有焦糊味", false));
        RepairTicketVO ticket = repairTicketService.confirmCreate(payload);

        assertThat(payload.safetyRisk()).isTrue();
        assertThat(payload.safetyNotices()).containsExactly("请立即停止使用设备，避免自行维修，并联系管理员。");
        assertThat(ticket.safetyRisk()).isTrue();
        assertThat(ticket.status()).isEqualTo(RepairTicketStatus.SUBMITTED);
        assertThat(ticket.createdAt()).isNotNull();
        assertThat(ticket.createdAt().getOffset().getTotalSeconds()).isEqualTo(8 * 60 * 60);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM lab WHERE id = 'LAB-B402'", String.class))
                .isEqualTo("ACTIVE");
    }

    @Test
    void shouldIsolateMineAndRejectStudentProcessing() {
        authenticate(1L, UserRole.STUDENT);
        RepairTicketVO ticket = createTicket("LAB-A301", "显示器", "无法点亮");

        authenticate(2L, UserRole.STUDENT);
        assertThat(repairTicketService.findMine(new RepairTicketQueryDTO(null, null, null)).total()).isZero();
        assertThatThrownBy(() -> repairTicketService.process(ticket.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.PROCESSING, "开始排查")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.FORBIDDEN));
    }

    @Test
    void shouldAllowOnlyAdjacentAdminTransitionsWithResolutionNote() {
        authenticate(1L, UserRole.STUDENT);
        RepairTicketVO submitted = createTicket("LAB-A301", "投影仪", "无法开机");

        authenticate(3L, UserRole.ADMIN);
        assertThatThrownBy(() -> repairTicketService.process(submitted.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.RESOLVED, "直接关闭工单")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.INVALID_TICKET_TRANSITION));
        assertThatThrownBy(() -> repairTicketService.process(submitted.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.PROCESSING, "  ")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.RESOLUTION_NOTE_REQUIRED));

        RepairTicketVO processing = repairTicketService.process(submitted.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.PROCESSING, "已安排现场检查"));
        RepairTicketVO resolved = repairTicketService.process(submitted.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.RESOLVED, "已更换电源模块并完成测试"));

        assertThat(processing.status()).isEqualTo(RepairTicketStatus.PROCESSING);
        assertThat(processing.resolutionNote()).isEqualTo("已安排现场检查");
        assertThat(resolved.status()).isEqualTo(RepairTicketStatus.RESOLVED);
        assertThat(resolved.resolutionNote()).isEqualTo("已更换电源模块并完成测试");
        assertThat(jdbcTemplate.queryForObject("SELECT processed_by FROM repair_ticket WHERE id = ?", Long.class,
                submitted.id())).isEqualTo(3L);
        assertThatThrownBy(() -> repairTicketService.process(submitted.id(),
                new RepairTicketUpdateDTO(RepairTicketStatus.RESOLVED, "重复处理")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.INVALID_TICKET_TRANSITION));
    }

    private RepairTicketVO createTicket(String labId, String equipmentInfo, String description) {
        RepairTicketCreatePayload payload = repairTicketService.prepareCreate(
                new RepairDraftCreateDTO(labId, equipmentInfo, description, false));
        return repairTicketService.confirmCreate(payload);
    }

    private static void authenticate(Long userId, UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(userId, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }
}
