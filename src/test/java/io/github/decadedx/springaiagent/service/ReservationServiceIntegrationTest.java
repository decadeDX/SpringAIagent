package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.ReservationCancelPayload;
import io.github.decadedx.springaiagent.dto.ReservationCreatePayload;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.entity.SysUser;
import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.ReservationSlotMapper;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.vo.ReservationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({TestcontainersConfiguration.class, ReservationServiceIntegrationTest.FixedClockConfiguration.class})
@SpringBootTest
class ReservationServiceIntegrationTest {

    private static final OffsetDateTime TUESDAY_NINE = OffsetDateTime.parse("2026-09-22T09:00:00+08:00");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationSlotMapper reservationSlotMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
        insertTrainedStudent(4L, "student03");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id >= 4");
    }

    @Test
    void shouldRejectUntrainedStudentBeforePreparingB402Reservation() {
        authenticate(2L);

        assertThatThrownBy(() -> reservationService.prepareCreate(createDraft("LAB-B402", TUESDAY_NINE, 2, 3)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.TRAINING_REQUIRED));
    }

    @Test
    void shouldRejectWeekendAndOverCapacityReservation() {
        authenticate(1L);

        assertThatThrownBy(() -> reservationService.prepareCreate(createDraft("LAB-A301",
                OffsetDateTime.parse("2026-09-26T09:00:00+08:00"), 1, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.INVALID_RESERVATION_TIME));
        assertThatThrownBy(() -> reservationService.prepareCreate(createDraft("LAB-B402", TUESDAY_NINE, 1, 13)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.BUSINESS_RULE_VIOLATION));
    }

    @Test
    void shouldAllowAdjacentReservationsAndReleaseCancelledSlots() {
        authenticate(1L);
        ReservationVO first = reservationService.confirmCreate(prepare("LAB-B402", TUESDAY_NINE, 2, 3));
        authenticate(4L);
        ReservationVO second = reservationService.confirmCreate(prepare("LAB-B402", TUESDAY_NINE.plusHours(2), 2, 3));

        assertThat(first.status().name()).isEqualTo("CONFIRMED");
        assertThat(second.status().name()).isEqualTo("CONFIRMED");
        assertThat(jdbcTemplate.queryForList("SELECT slot_start_time FROM reservation_slot ORDER BY slot_start_time",
                LocalDateTime.class)).containsExactly(
                LocalDateTime.of(2026, 9, 22, 1, 0),
                LocalDateTime.of(2026, 9, 22, 2, 0),
                LocalDateTime.of(2026, 9, 22, 3, 0),
                LocalDateTime.of(2026, 9, 22, 4, 0));
        assertThat(reservationSlotMapper.selectOccupiedSlotStarts("LAB-B402",
                LocalDateTime.of(2026, 9, 22, 1, 0), LocalDateTime.of(2026, 9, 22, 5, 0))).hasSize(4);

        authenticate(1L);
        ReservationVO cancelled = reservationService.confirmCancel(new ReservationCancelPayload(first.id()));

        assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
        assertThat(reservationSlotMapper.selectOccupiedSlotStarts("LAB-B402",
                LocalDateTime.of(2026, 9, 22, 1, 0), LocalDateTime.of(2026, 9, 22, 5, 0)))
                .containsExactly(LocalDateTime.of(2026, 9, 22, 3, 0), LocalDateTime.of(2026, 9, 22, 4, 0));
    }

    @Test
    void shouldKeepCurrentUserToTwoActiveReservations() {
        authenticate(1L);
        reservationService.confirmCreate(prepare("LAB-A301", TUESDAY_NINE, 1, 1));
        reservationService.confirmCreate(prepare("LAB-A301", TUESDAY_NINE.plusHours(1), 1, 1));

        assertThatThrownBy(() -> reservationService.confirmCreate(prepare("LAB-A301", TUESDAY_NINE.plusHours(2), 1, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.RESERVATION_LIMIT_REACHED));
    }

    @Test
    void shouldAllowOnlyOneConcurrentReservationForSameLabSlot() throws Exception {
        List<Long> userIds = new ArrayList<>();
        for (long userId = 10; userId < 30; userId++) {
            insertTrainedStudent(userId, "concurrent" + userId);
            userIds.add(userId);
        }
        ReservationCreatePayload payload = new ReservationCreatePayload("LAB-B402", "人工智能实验室",
                TUESDAY_NINE.plusHours(5), TUESDAY_NINE.plusHours(7), 3);
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(userIds.size());
        try {
            List<Future<ApiCode>> results = new ArrayList<>();
            for (Long userId : userIds) {
                results.add(executor.submit(() -> {
                    authenticate(userId);
                    ready.countDown();
                    start.await();
                    try {
                        reservationService.confirmCreate(payload);
                        return ApiCode.OK;
                    } catch (BusinessException exception) {
                        return exception.getCode();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            ready.await();
            start.countDown();
            List<ApiCode> outcomes = new ArrayList<>();
            for (Future<ApiCode> result : results) {
                outcomes.add(result.get());
            }

            assertThat(outcomes).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.Stream.concat(java.util.stream.Stream.of(ApiCode.OK),
                            java.util.stream.Stream.generate(() -> ApiCode.RESERVATION_CONFLICT).limit(19)).toList());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation", Long.class)).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot", Long.class)).isEqualTo(2L);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot s "
                    + "LEFT JOIN reservation r ON r.id = s.reservation_id WHERE r.id IS NULL", Long.class)).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private ReservationCreatePayload prepare(String labId, OffsetDateTime startTime, int durationHours,
                                             int participantCount) {
        return reservationService.prepareCreate(createDraft(labId, startTime, durationHours, participantCount));
    }

    private ReservationDraftCreateDTO createDraft(String labId, OffsetDateTime startTime, int durationHours,
                                                   int participantCount) {
        return new ReservationDraftCreateDTO(labId, startTime, startTime.plusHours(durationHours), participantCount);
    }

    private void insertTrainedStudent(Long id, String username) {
        if (sysUserMapper.selectById(id) != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi");
        user.setRole(UserRole.STUDENT);
        user.setTrainingStatus(TrainingStatus.PASSED);
        sysUserMapper.insert(user);
    }

    private static void authenticate(Long userId) {
        AuthenticatedUser user = new AuthenticatedUser(userId, UserRole.STUDENT);
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
