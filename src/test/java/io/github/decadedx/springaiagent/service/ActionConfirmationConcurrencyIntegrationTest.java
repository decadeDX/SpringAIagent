package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.ActionConfirmDTO;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.entity.SysUser;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import io.github.decadedx.springaiagent.vo.ActionExecutionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证二十位不同已培训学生按“草案—确认”链路竞争同一实验室时段时，数据库不会留下部分预约数据。
 */
@Import({TestcontainersConfiguration.class, ActionConfirmationConcurrencyIntegrationTest.FixedClockConfiguration.class})
@SpringBootTest
class ActionConfirmationConcurrencyIntegrationTest {

    /** 与固定业务时钟匹配的可预约人工智能实验室时段。 */
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2026-09-22T14:00:00+08:00");

    /** 并发参与学生数量，满足课程验收下限。 */
    private static final int CONCURRENT_USER_COUNT = 20;

    /** 预约草案准备服务。 */
    @Autowired
    private ReservationService reservationService;

    /** Redis 草案服务。 */
    @Autowired
    private ActionDraftService actionDraftService;

    /** 唯一允许写入业务事实的确认服务。 */
    @Autowired
    private ActionConfirmationService actionConfirmationService;

    /** 创建隔离测试学生的 Mapper。 */
    @Autowired
    private SysUserMapper sysUserMapper;

    /** 对最终预约、时隙和动作执行记录做数据库一致性断言。 */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 清理本测试生成的短期草案。 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 每个用例开始前清除本用例产生的业务事实。 */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM action_execution WHERE user_id BETWEEN 1001 AND 1020");
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id BETWEEN 1001 AND 1020");
        clearActionDrafts();
    }

    /** 每个用例结束后恢复认证上下文和隔离数据。 */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM action_execution WHERE user_id BETWEEN 1001 AND 1020");
        jdbcTemplate.update("DELETE FROM reservation_slot");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM sys_user WHERE id BETWEEN 1001 AND 1020");
        clearActionDrafts();
    }

    /**
     * 二十个用户先各自取得草案，再同时确认；只有一人可以占用两个整点时隙。
     *
     * @throws Exception 并发任务未能完成时测试失败
     */
    @Test
    void shouldKeepReservationsSlotsAndActionExecutionsConsistentWhenTwentyDraftsConfirmConcurrently() throws Exception {
        List<ActionDraftVO> drafts = createDraftsForTwentyTrainedStudents();
        CountDownLatch ready = new CountDownLatch(CONCURRENT_USER_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USER_COUNT);
        try {
            List<Future<ApiCode>> confirmations = new ArrayList<>();
            for (int index = 0; index < drafts.size(); index++) {
                long userId = 1001L + index;
                ActionDraftVO draft = drafts.get(index);
                confirmations.add(executor.submit(() -> confirmWhenStarted(userId, draft, ready, start)));
            }
            ready.await();
            start.countDown();

            List<ApiCode> outcomes = new ArrayList<>();
            for (Future<ApiCode> confirmation : confirmations) {
                outcomes.add(confirmation.get());
            }

            assertThat(outcomes).containsOnly(ApiCode.OK, ApiCode.RESERVATION_CONFLICT);
            assertThat(outcomes.stream().filter(ApiCode.OK::equals)).hasSize(1);
            assertThat(outcomes.stream().filter(ApiCode.RESERVATION_CONFLICT::equals)).hasSize(19);
            assertDatabaseHasOneCompleteReservation();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 同一学生并发提交不同时间的三份草案时，用户行锁必须保证最多两条有效预约。
     *
     * @throws Exception 并发任务未能完成时测试失败
     */
    @Test
    void shouldKeepOneStudentsConcurrentConfirmedReservationsWithinTwoReservationLimit() throws Exception {
        long userId = 1001L;
        insertTrainedStudent(userId);
        List<ActionDraftVO> drafts = List.of(
                createDraft(userId, START_TIME),
                createDraft(userId, START_TIME.plusHours(2)),
                createDraft(userId, START_TIME.plusHours(4)));
        CountDownLatch ready = new CountDownLatch(drafts.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(drafts.size());
        try {
            List<Future<ApiCode>> confirmations = new ArrayList<>();
            for (ActionDraftVO draft : drafts) {
                confirmations.add(executor.submit(() -> confirmWhenStarted(userId, draft, ready, start)));
            }
            ready.await();
            start.countDown();

            List<ApiCode> outcomes = new ArrayList<>();
            for (Future<ApiCode> confirmation : confirmations) {
                outcomes.add(confirmation.get());
            }
            assertThat(outcomes).containsExactlyInAnyOrder(ApiCode.OK, ApiCode.OK, ApiCode.RESERVATION_LIMIT_REACHED);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation WHERE user_id = 1001", Long.class))
                    .isEqualTo(2L);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot", Long.class)).isEqualTo(4L);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 为二十位已培训学生逐一生成同一时段的独立草案；生成草案本身不应占用时隙。
     *
     * @return 与学生编号顺序一致的草案
     */
    private List<ActionDraftVO> createDraftsForTwentyTrainedStudents() {
        List<ActionDraftVO> drafts = new ArrayList<>();
        for (long userId = 1001L; userId < 1001L + CONCURRENT_USER_COUNT; userId++) {
            insertTrainedStudent(userId);
            authenticate(userId);
            try {
                drafts.add(actionDraftService.create(ActionType.CREATE_RESERVATION,
                        "concurrency-" + userId,
                        reservationService.prepareCreate(new ReservationDraftCreateDTO("LAB-B402", START_TIME,
                                START_TIME.plusHours(2), 3)), List.of()));
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation", Long.class)).isZero();
        return drafts;
    }

    /**
     * 用独立会话创建同一用户的一份预约草案，避免“同会话同动作只保留最新草案”的规则干扰并发上限测试。
     *
     * @param userId 草案所属学生
     * @param startTime 预约开始时间
     * @return 待确认草案
     */
    private ActionDraftVO createDraft(long userId, OffsetDateTime startTime) {
        authenticate(userId);
        try {
            return actionDraftService.create(ActionType.CREATE_RESERVATION,
                    "same-user-" + startTime.getHour(),
                    reservationService.prepareCreate(new ReservationDraftCreateDTO("LAB-B402", startTime,
                            startTime.plusHours(2), 3)), List.of());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 等待所有确认任务就绪后提交确认请求，并将业务错误转换为可断言的稳定错误码。
     *
     * @param userId 草案所属学生
     * @param draft 待确认草案
     * @param ready 就绪屏障
     * @param start 放行屏障
     * @return 成功或预约冲突错误码
     * @throws InterruptedException 等待屏障被中断时抛出
     */
    private ApiCode confirmWhenStarted(long userId, ActionDraftVO draft, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        authenticate(userId);
        ready.countDown();
        start.await();
        try {
            ActionExecutionVO execution = actionConfirmationService.confirm(draft.actionId(),
                    new ActionConfirmDTO(draft.sessionId()));
            return execution.executionStatus().name().equals("SUCCEEDED") ? ApiCode.OK : ApiCode.BUSINESS_CONFLICT;
        } catch (BusinessException exception) {
            return exception.getCode();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** 验证预约、时隙和动作执行记录没有孤立或重复数据。 */
    private void assertDatabaseHasOneCompleteReservation() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot s "
                + "LEFT JOIN reservation r ON r.id = s.reservation_id WHERE r.id IS NULL", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_slot "
                + "WHERE lab_id = 'LAB-B402' AND slot_start_time IN ('2026-09-22 06:00:00', '2026-09-22 07:00:00')",
                Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM action_execution "
                + "WHERE user_id BETWEEN 1001 AND 1020 AND execution_status = 'SUCCEEDED'", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM action_execution "
                + "WHERE user_id BETWEEN 1001 AND 1020 AND execution_status = 'FAILED'", Long.class)).isEqualTo(19L);
    }

    /**
     * 写入一位已通过培训的测试学生。
     *
     * @param userId 测试用户主键
     */
    private void insertTrainedStudent(long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername("concurrency" + userId);
        user.setPasswordHash("$2a$10$CRP4LoVva1mN5gjGPNlKtOaleaoh1XQKxjl8NemcTCw0v/9GVmrbi");
        user.setRole(UserRole.STUDENT);
        user.setTrainingStatus(TrainingStatus.PASSED);
        sysUserMapper.insert(user);
    }

    /**
     * 将当前线程模拟为指定学生，确保服务层只从认证上下文读取用户身份。
     *
     * @param userId 当前学生主键
     */
    private void authenticate(long userId) {
        AuthenticatedUser user = new AuthenticatedUser(userId, UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
    }

    /** 清除所有短期草案键，避免 Redis 中的过期数据影响独立集成测试。 */
    private void clearActionDrafts() {
        Set<String> keys = stringRedisTemplate.keys("agent:action:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /** 为预约时间规则固定一个周一上海时间。 */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * 提供固定业务时钟，保证测试日期始终位于七天预约窗口中。
         *
         * @return 上海时区的固定时钟
         */
        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), TimeConfig.BUSINESS_ZONE);
        }
    }
}
