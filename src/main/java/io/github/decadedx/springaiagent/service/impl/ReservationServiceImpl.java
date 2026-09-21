package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.ReservationCancelPayload;
import io.github.decadedx.springaiagent.dto.ReservationCreatePayload;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.ReservationQueryDTO;
import io.github.decadedx.springaiagent.entity.Lab;
import io.github.decadedx.springaiagent.entity.Reservation;
import io.github.decadedx.springaiagent.entity.ReservationSlot;
import io.github.decadedx.springaiagent.entity.SysUser;
import io.github.decadedx.springaiagent.enums.LabStatus;
import io.github.decadedx.springaiagent.enums.ReservationStatus;
import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.LabMapper;
import io.github.decadedx.springaiagent.mapper.ReservationMapper;
import io.github.decadedx.springaiagent.mapper.ReservationSlotMapper;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.ReservationService;
import io.github.decadedx.springaiagent.vo.ReservationPageVO;
import io.github.decadedx.springaiagent.vo.ReservationVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行预约业务规则和数据库事务；所有确认操作均以数据库当前状态作为最终依据。
 */
@Service
public class ReservationServiceImpl implements ReservationService {

    /** 数据库存储预约时刻使用的 UTC 偏移。 */
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /** 对外输入和输出强制使用的上海固定偏移。 */
    private static final ZoneOffset SHANGHAI_OFFSET = ZoneOffset.ofHours(8);

    /** 人工智能与嵌入式实验室需要通过培训。 */
    private static final List<String> TRAINING_REQUIRED_LAB_IDS = List.of("LAB-B402", "LAB-C205");

    /** 实验室资料访问入口。 */
    private final LabMapper labMapper;

    /** 预约聚合根访问入口。 */
    private final ReservationMapper reservationMapper;

    /** 预约时隙访问入口。 */
    private final ReservationSlotMapper reservationSlotMapper;

    /** 用户访问入口，用于锁定用户并读取最新培训状态。 */
    private final SysUserMapper sysUserMapper;

    /** 统一业务时钟，生产环境使用上海当前时刻，测试可固定。 */
    private final Clock clock;

    /**
     * 创建预约领域服务。
     *
     * @param labMapper 实验室资料 Mapper
     * @param reservationMapper 预约 Mapper
     * @param reservationSlotMapper 时隙 Mapper
     * @param sysUserMapper 用户 Mapper
     * @param clock 业务时钟
     */
    public ReservationServiceImpl(LabMapper labMapper, ReservationMapper reservationMapper,
                                  ReservationSlotMapper reservationSlotMapper, SysUserMapper sysUserMapper,
                                  Clock clock) {
        this.labMapper = labMapper;
        this.reservationMapper = reservationMapper;
        this.reservationSlotMapper = reservationSlotMapper;
        this.sysUserMapper = sysUserMapper;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationCreatePayload prepareCreate(ReservationDraftCreateDTO createDTO) {
        Long userId = requireStudentId();
        SysUser user = requireUser(userId);
        validateCreateInput(createDTO);
        Lab lab = validateReservation(createDTO.labId(), createDTO.startTime(), createDTO.endTime(),
                createDTO.participantCount(), user);
        return new ReservationCreatePayload(lab.getId(), lab.getName(), normalizeShanghai(createDTO.startTime()),
                normalizeShanghai(createDTO.endTime()), createDTO.participantCount());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO confirmCreate(ReservationCreatePayload payload) {
        Long userId = requireStudentId();
        validatePayload(payload);
        SysUser user = requireLockedUser(userId);
        Lab lab = validateReservation(payload.labId(), payload.startTime(), payload.endTime(),
                payload.participantCount(), user);
        LocalDateTime nowUtc = toUtc(now());
        if (reservationMapper.countActiveByUser(userId, nowUtc) >= 2) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.RESERVATION_LIMIT_REACHED,
                    "当前有效预约已达两条上限");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setLabId(lab.getId());
        reservation.setStartTime(toUtc(payload.startTime()));
        reservation.setEndTime(toUtc(payload.endTime()));
        reservation.setParticipantCount(payload.participantCount());
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationMapper.insert(reservation);

        try {
            reservationSlotMapper.insertBatch(createSlots(reservation, payload.startTime(), payload.endTime()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.RESERVATION_CONFLICT,
                    "该实验室在指定时段已被预约，请重新选择");
        }
        return toVO(reservation, lab.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationCancelPayload prepareCancel(Long reservationId) {
        Long userId = requireStudentId();
        if (reservationId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "预约编号不能为空");
        }
        Reservation reservation = reservationMapper.selectById(reservationId);
        validateCancelable(reservation, userId);
        return new ReservationCancelPayload(reservationId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO confirmCancel(ReservationCancelPayload payload) {
        Long userId = requireStudentId();
        if (payload == null || payload.reservationId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "预约编号不能为空");
        }
        Reservation reservation = reservationMapper.selectByIdForUpdate(payload.reservationId());
        validateCancelable(reservation, userId);
        LocalDateTime cancelledAt = toUtc(now());
        if (reservationMapper.cancelConfirmed(reservation.getId(), cancelledAt) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "预约状态已变化，请刷新后重试");
        }
        reservationSlotMapper.deleteByReservationId(reservation.getId());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(cancelledAt);
        Lab lab = requireLab(reservation.getLabId());
        return toVO(reservation, lab.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReservationPageVO findMine(ReservationQueryDTO queryDTO) {
        Long userId = requireStudentId();
        if (queryDTO.from() != null && queryDTO.to() != null && !queryDTO.from().isBefore(queryDTO.to())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "查询起始时间必须早于结束时间");
        }
        int page = queryDTO.page() == null ? 1 : queryDTO.page();
        int size = queryDTO.size() == null ? 20 : queryDTO.size();
        LocalDateTime from = queryDTO.from() == null ? null : toUtc(queryDTO.from());
        LocalDateTime to = queryDTO.to() == null ? null : toUtc(queryDTO.to());
        long total = reservationMapper.countMine(userId, queryDTO.status(), from, to);
        List<ReservationVO> items = reservationMapper.selectMine(userId, queryDTO.status(), from, to,
                        (long) (page - 1) * size, size)
                .stream()
                .map(reservation -> toVO(reservation, reservation.getLabName()))
                .toList();
        return new ReservationPageVO(items, page, size, total);
    }

    /**
     * 验证 Controller 或工具调用进入服务时必须具备的创建字段。
     *
     * @param createDTO 原始预约输入
     */
    private void validateCreateInput(ReservationDraftCreateDTO createDTO) {
        if (createDTO == null || !StringUtils.hasText(createDTO.labId()) || createDTO.startTime() == null
                || createDTO.endTime() == null || createDTO.participantCount() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "预约参数不完整");
        }
    }

    /**
     * 验证来自草案存储的确认载荷字段完整性。
     *
     * @param payload 确认阶段恢复的预约载荷
     */
    private void validatePayload(ReservationCreatePayload payload) {
        if (payload == null || !StringUtils.hasText(payload.labId()) || payload.startTime() == null
                || payload.endTime() == null || payload.participantCount() <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "预约草案参数不完整");
        }
    }

    /**
     * 检查预约时间、实验室状态、容量和培训要求；确认阶段调用此方法以重新读取数据库规则。
     *
     * @param labId 实验室业务编号
     * @param startTime 带时区偏移的开始时间
     * @param endTime 带时区偏移的结束时间
     * @param participantCount 参与人数
     * @param user 当前预约人及其最新培训状态
     * @return 已确认存在且当前可预约的实验室
     */
    private Lab validateReservation(String labId, OffsetDateTime startTime, OffsetDateTime endTime,
                                    int participantCount, SysUser user) {
        Lab lab = requireLab(labId);
        if (lab.getStatus() != LabStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.LAB_MAINTENANCE,
                    "实验室当前不可预约");
        }
        if (participantCount <= 0 || participantCount > lab.getCapacity()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.BUSINESS_RULE_VIOLATION,
                    "预约人数必须在实验室容量范围内");
        }
        validateTime(lab, startTime, endTime);
        if (TRAINING_REQUIRED_LAB_IDS.contains(lab.getId()) && user.getTrainingStatus() != TrainingStatus.PASSED) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.TRAINING_REQUIRED,
                    "预约该实验室前需要通过安全培训");
        }
        return lab;
    }

    /**
     * 校验预约时刻满足上海时区、整点、时长、工作日、预约窗口和开放时段规则。
     *
     * @param lab 已存在实验室
     * @param startTime 预约开始时间
     * @param endTime 预约结束时间
     */
    private void validateTime(Lab lab, OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null || !SHANGHAI_OFFSET.equals(startTime.getOffset())
                || !SHANGHAI_OFFSET.equals(endTime.getOffset())) {
            invalidReservationTime();
        }
        OffsetDateTime normalizedStart = normalizeShanghai(startTime);
        OffsetDateTime normalizedEnd = normalizeShanghai(endTime);
        Duration duration = Duration.between(normalizedStart.toInstant(), normalizedEnd.toInstant());
        if (normalizedStart.getMinute() != 0 || normalizedStart.getSecond() != 0 || normalizedStart.getNano() != 0
                || duration.isNegative() || duration.isZero() || duration.toMinutes() % 60 != 0
                || duration.toHours() < 1 || duration.toHours() > 3
                || !normalizedStart.toLocalDate().equals(normalizedEnd.toLocalDate())
                || isWeekend(normalizedStart.getDayOfWeek())
                || !normalizedStart.isAfter(now())
                || normalizedStart.isAfter(now().plusHours(7 * 24L))
                || normalizedStart.toLocalTime().isBefore(lab.getOpenTime())
                || normalizedEnd.toLocalTime().isAfter(lab.getCloseTime())) {
            invalidReservationTime();
        }
    }

    /**
     * 检查取消目标存在、属于当前用户、仍有效且距离开始不少于三十分钟。
     *
     * @param reservation 待取消预约
     * @param userId 当前认证用户主键
     */
    private void validateCancelable(Reservation reservation, Long userId) {
        if (reservation == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.NOT_FOUND, "预约不存在");
        }
        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.RESERVATION_NOT_OWNED, "该预约不属于当前用户");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "该预约当前不能取消");
        }
        OffsetDateTime startTime = toShanghai(reservation.getStartTime());
        if (startTime.isBefore(now().plusMinutes(30))) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.CANCELLATION_TOO_LATE,
                    "预约开始前不足30分钟，不能取消");
        }
    }

    /**
     * 读取当前用户；账号被删除时视为认证失效。
     *
     * @param userId 当前认证用户主键
     * @return 当前用户资料
     */
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHENTICATED, "请先登录");
        }
        return user;
    }

    /**
     * 锁定当前用户行并读取最新培训状态，用于串行化预约额度检查。
     *
     * @param userId 当前认证用户主键
     * @return 已锁定用户资料
     */
    private SysUser requireLockedUser(Long userId) {
        SysUser user = sysUserMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHENTICATED, "请先登录");
        }
        return user;
    }

    /**
     * 获取实验室，不存在时返回稳定错误码。
     *
     * @param labId 实验室业务编号
     * @return 实验室资料
     */
    private Lab requireLab(String labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.LAB_NOT_FOUND, "实验室不存在");
        }
        return lab;
    }

    /**
     * 从认证上下文取得学生身份，拒绝角色不足的内部调用。
     *
     * @return 当前认证学生主键
     */
    private Long requireStudentId() {
        if (CurrentUser.requireRole() != UserRole.STUDENT) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "仅学生可以办理预约");
        }
        return CurrentUser.requireId();
    }

    /**
     * 生成预约覆盖的每个整点时隙，区间采用左闭右开语义。
     *
     * @param reservation 已插入预约聚合根
     * @param startTime 上海开始时刻
     * @param endTime 上海结束时刻
     * @return 待批量写入的时隙
     */
    private List<ReservationSlot> createSlots(Reservation reservation, OffsetDateTime startTime,
                                              OffsetDateTime endTime) {
        List<ReservationSlot> slots = new ArrayList<>();
        for (OffsetDateTime slotStart = normalizeShanghai(startTime);
             slotStart.isBefore(normalizeShanghai(endTime)); slotStart = slotStart.plusHours(1)) {
            ReservationSlot slot = new ReservationSlot();
            slot.setId(IdWorker.getId());
            slot.setLabId(reservation.getLabId());
            slot.setReservationId(reservation.getId());
            slot.setSlotStartTime(toUtc(slotStart));
            slots.add(slot);
        }
        return slots;
    }

    /**
     * 将预约实体转换为上海时区的接口对象。
     *
     * @param reservation 预约实体
     * @param labName 实验室名称
     * @return 对外预约摘要
     */
    private ReservationVO toVO(Reservation reservation, String labName) {
        return new ReservationVO(reservation.getId(), "RSV-" + reservation.getId(), reservation.getLabId(), labName,
                toShanghai(reservation.getStartTime()), toShanghai(reservation.getEndTime()),
                reservation.getParticipantCount(), reservation.getStatus(),
                reservation.getCancelledAt() == null ? null : toShanghai(reservation.getCancelledAt()));
    }

    /**
     * 返回上海时区当前时刻。
     *
     * @return 可被测试时钟固定的业务当前时刻
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(SHANGHAI_OFFSET);
    }

    /**
     * 校验给定日期是否为周末。
     *
     * @param dayOfWeek 待校验星期
     * @return 周六或周日时为 true
     */
    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 统一抛出预约时间规则不满足的业务错误。
     */
    private void invalidReservationTime() {
        throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.INVALID_RESERVATION_TIME,
                "预约时间不符合整点、时长、工作日或开放时段规则");
    }

    /**
     * 将输入时间转换为固定 +08:00 偏移表示。
     *
     * @param value 带偏移时间
     * @return 上海偏移时间
     */
    private OffsetDateTime normalizeShanghai(OffsetDateTime value) {
        return value.withOffsetSameInstant(SHANGHAI_OFFSET);
    }

    /**
     * 将带偏移时间转换为数据库存储使用的 UTC LocalDateTime。
     *
     * @param value 带偏移时间
     * @return UTC LocalDateTime
     */
    private LocalDateTime toUtc(OffsetDateTime value) {
        return value.withOffsetSameInstant(UTC).toLocalDateTime();
    }

    /**
     * 将数据库 UTC LocalDateTime 转为接口使用的上海偏移时间。
     *
     * @param value UTC LocalDateTime
     * @return +08:00 偏移时间
     */
    private OffsetDateTime toShanghai(LocalDateTime value) {
        return value.atOffset(UTC).atZoneSameInstant(TimeConfig.BUSINESS_ZONE).toOffsetDateTime();
    }
}
