package io.github.decadedx.springaiagent.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Import({TestcontainersConfiguration.class, ReservationControllerIntegrationTest.FixedClockConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
        clearRedisKeys("auth:session:*");
        clearRedisKeys("agent:action:*");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
    }

    @Test
    void shouldReturnOnlyCurrentStudentsReservations() throws Exception {
        authenticateStudentOne();
        OffsetDateTime startTime = OffsetDateTime.parse("2026-09-22T14:00:00+08:00");
        reservationService.confirmCreate(reservationService.prepareCreate(
                new ReservationDraftCreateDTO("LAB-B402", startTime, startTime.plusHours(2), 3)));
        SecurityContextHolder.clearContext();

        String studentOneToken = login("student01");
        MvcResult reservationResult = mockMvc.perform(get("/api/reservations/me")
                        .header("Authorization", "Bearer " + studentOneToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").isString())
                .andExpect(jsonPath("$.data.items[0].labId").value("LAB-B402"))
                .andExpect(jsonPath("$.data.items[0].startTime").value("2026-09-22T14:00:00+08:00"))
                .andReturn();
        String reservationId = JsonPath.read(reservationResult.getResponse().getContentAsString(), "$.data.items[0].id");
        MvcResult cancellationDraftResult = mockMvc.perform(post("/api/reservations/{reservationId}/cancellation-draft", reservationId)
                        .header("Authorization", "Bearer " + studentOneToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payload.reservationId").isString())
                .andReturn();
        String cancellationDraftBody = cancellationDraftResult.getResponse().getContentAsString();
        String actionId = JsonPath.read(cancellationDraftBody, "$.data.actionId");
        String sessionId = JsonPath.read(cancellationDraftBody, "$.data.sessionId");
        mockMvc.perform(post("/api/actions/{actionId}/confirm", actionId)
                        .header("Authorization", "Bearer " + studentOneToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sessionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.businessId").isString())
                .andExpect(jsonPath("$.data.result.reservationId").isString());
        mockMvc.perform(get("/api/reservations/me").header("Authorization", "Bearer " + login("student02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void shouldCreateReservationDraftWhenRequestUsesShanghaiOffset() throws Exception {
        mockMvc.perform(post("/api/reservation-drafts")
                        .with(authentication(studentOneAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labId\":\"LAB-A301\",\"startTime\":\"2026-09-22T09:00:00+08:00\","
                                + "\"endTime\":\"2026-09-22T10:00:00+08:00\",\"participantCount\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payload.startTime").value("2026-09-22T09:00:00+08:00"))
                .andExpect(jsonPath("$.data.payload.endTime").value("2026-09-22T10:00:00+08:00"));
    }

    @Test
    void shouldConfirmAndReplayReservationWithStringIds() throws Exception {
        String token = login("student01");
        MvcResult draftResult = mockMvc.perform(post("/api/reservation-drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labId\":\"LAB-A301\",\"startTime\":\"2026-09-22T09:00:00+08:00\","
                                + "\"endTime\":\"2026-09-22T10:00:00+08:00\",\"participantCount\":1}"))
                .andExpect(status().isCreated())
                .andReturn();
        String draftBody = draftResult.getResponse().getContentAsString();
        String actionId = JsonPath.read(draftBody, "$.data.actionId");
        String sessionId = JsonPath.read(draftBody, "$.data.sessionId");
        String confirmBody = "{\"sessionId\":\"" + sessionId + "\"}";

        mockMvc.perform(post("/api/actions/{actionId}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotentReplay").value(false))
                .andExpect(jsonPath("$.data.result.businessId").isString())
                .andExpect(jsonPath("$.data.result.reservationId").isString());
        mockMvc.perform(post("/api/actions/{actionId}/confirm", actionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idempotentReplay").value(true))
                .andExpect(jsonPath("$.data.result.businessId").isString())
                .andExpect(jsonPath("$.data.result.reservationId").isString());
    }

    /** 清理本类测试创建的短期认证和草案 Redis 键。 */
    private void clearRedisKeys(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private void authenticateStudentOne() {
        SecurityContextHolder.getContext().setAuthentication(studentOneAuthentication());
    }

    private UsernamePasswordAuthenticationToken studentOneAuthentication() {
        AuthenticatedUser user = new AuthenticatedUser(1L, UserRole.STUDENT);
        return new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), TimeConfig.BUSINESS_ZONE);
        }
    }
}
