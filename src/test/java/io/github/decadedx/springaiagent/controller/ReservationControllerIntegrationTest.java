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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
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

        mockMvc.perform(get("/api/reservations/me").header("Authorization", "Bearer " + login("student01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].labId").value("LAB-B402"))
                .andExpect(jsonPath("$.data.items[0].startTime").value("2026-09-22T14:00:00+08:00"));
        mockMvc.perform(get("/api/reservations/me").header("Authorization", "Bearer " + login("student02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
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
        AuthenticatedUser user = new AuthenticatedUser(1L, UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
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
