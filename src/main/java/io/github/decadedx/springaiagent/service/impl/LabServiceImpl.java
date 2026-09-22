package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.LabAvailabilityQueryDTO;
import io.github.decadedx.springaiagent.dto.LabQueryDTO;
import io.github.decadedx.springaiagent.dto.LabUpdateDTO;
import io.github.decadedx.springaiagent.entity.Lab;
import io.github.decadedx.springaiagent.enums.LabStatus;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.LabMapper;
import io.github.decadedx.springaiagent.mapper.ReservationSlotMapper;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.LabDetailCache;
import io.github.decadedx.springaiagent.service.LabService;
import io.github.decadedx.springaiagent.vo.AvailabilitySlotVO;
import io.github.decadedx.springaiagent.vo.LabAvailabilityVO;
import io.github.decadedx.springaiagent.vo.LabPageVO;
import io.github.decadedx.springaiagent.vo.LabVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 从 MySQL 读取实验室资料和时隙占用，并在服务边界转换 UTC 与上海本地时间。
 */
@Service
public class LabServiceImpl implements LabService {

    /** 数据库 UTC 时间转换使用的固定偏移。 */
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /** 实验室基础资料访问入口。 */
    private final LabMapper labMapper;

    /** 时隙占用查询入口。 */
    private final ReservationSlotMapper reservationSlotMapper;

    /** 统一业务时钟，用于标记已经过去或超出预约窗口的时隙。 */
    private final Clock clock;

    /** 实验室详情缓存。 */
    private final LabDetailCache labDetailCache;

    /**
     * 创建实验室查询服务。
     *
     * @param labMapper 实验室资料 Mapper
     * @param reservationSlotMapper 时隙 Mapper
     * @param clock 业务时钟
     */
    public LabServiceImpl(LabMapper labMapper, ReservationSlotMapper reservationSlotMapper, Clock clock,
                          LabDetailCache labDetailCache) {
        this.labMapper = labMapper;
        this.reservationSlotMapper = reservationSlotMapper;
        this.clock = clock;
        this.labDetailCache = labDetailCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabPageVO search(LabQueryDTO queryDTO) {
        int page = queryDTO.page() == null ? 1 : queryDTO.page();
        int size = queryDTO.size() == null ? 20 : queryDTO.size();
        List<Lab> labs = labMapper.selectList(new LambdaQueryWrapper<Lab>()
                .like(StringUtils.hasText(queryDTO.name()), Lab::getName, queryDTO.name())
                .like(StringUtils.hasText(queryDTO.equipment()), Lab::getEquipmentDescription, queryDTO.equipment())
                .ge(queryDTO.minCapacity() != null, Lab::getCapacity, queryDTO.minCapacity())
                .orderByAsc(Lab::getId));
        int fromIndex = Math.min((page - 1) * size, labs.size());
        int toIndex = Math.min(fromIndex + size, labs.size());
        List<LabVO> items = labs.subList(fromIndex, toIndex).stream().map(this::toVO).toList();
        return new LabPageVO(items, page, size, labs.size());
    }

    /** {@inheritDoc} */
    @Override
    public LabVO findDetail(String labId) {
        return labDetailCache.get(labId).orElseGet(() -> {
            LabVO detail = toVO(requireLab(labId));
            labDetailCache.put(detail);
            return detail;
        });
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LabVO update(String labId, LabUpdateDTO updateDTO) {
        requireAdmin();
        if (updateDTO == null || (updateDTO.name() == null && updateDTO.capacity() == null
                && updateDTO.equipmentDescription() == null && updateDTO.openTime() == null
                && updateDTO.closeTime() == null && updateDTO.status() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "至少需要提供一个更新字段");
        }
        Lab lab = requireLab(labId);
        if (updateDTO.name() != null) {
            lab.setName(updateDTO.name().trim());
        }
        if (updateDTO.capacity() != null) {
            lab.setCapacity(updateDTO.capacity());
        }
        if (updateDTO.equipmentDescription() != null) {
            lab.setEquipmentDescription(updateDTO.equipmentDescription().trim());
        }
        if (updateDTO.openTime() != null) {
            lab.setOpenTime(updateDTO.openTime());
        }
        if (updateDTO.closeTime() != null) {
            lab.setCloseTime(updateDTO.closeTime());
        }
        if (updateDTO.status() != null) {
            lab.setStatus(updateDTO.status());
        }
        if (lab.getName().isBlank() || lab.getEquipmentDescription().isBlank() || !lab.getOpenTime().isBefore(lab.getCloseTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "实验室名称、设备说明和开放时间不合法");
        }
        labMapper.updateById(lab);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /** 数据库事务提交后才允许删除缓存。 */
            @Override
            public void afterCommit() {
                labDetailCache.evict(labId);
            }
        });
        return toVO(lab);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LabAvailabilityVO availability(String labId, LabAvailabilityQueryDTO queryDTO) {
        if (queryDTO.from() != null && queryDTO.to() != null && !queryDTO.from().isBefore(queryDTO.to())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "查询起始时间必须早于结束时间");
        }
        Lab lab = requireLab(labId);
        LocalTime start = latest(lab.getOpenTime(), queryDTO.from());
        LocalTime end = earliest(lab.getCloseTime(), queryDTO.to());
        LocalTime firstHour = roundUpToHour(start);
        LocalTime lastHour = end.withMinute(0).withSecond(0).withNano(0);
        if (!firstHour.isBefore(lastHour)) {
            return new LabAvailabilityVO(lab.getId(), queryDTO.date(), lab.getOpenTime(), lab.getCloseTime(), List.of());
        }

        LocalDateTime localStart = queryDTO.date().atTime(firstHour);
        LocalDateTime localEnd = queryDTO.date().atTime(lastHour);
        Set<LocalDateTime> occupied = new HashSet<>(reservationSlotMapper.selectOccupiedSlotStarts(
                lab.getId(), toUtc(localStart), toUtc(localEnd)));
        boolean reservableDate = queryDTO.date().getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                && queryDTO.date().getDayOfWeek() != java.time.DayOfWeek.SUNDAY;
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<AvailabilitySlotVO> slots = java.util.stream.Stream.iterate(localStart,
                        value -> value.isBefore(localEnd), value -> value.plusHours(1))
                .map(slotStart -> new AvailabilitySlotVO(
                        slotStart.atZone(TimeConfig.BUSINESS_ZONE).toOffsetDateTime(),
                        slotStart.plusHours(1).atZone(TimeConfig.BUSINESS_ZONE).toOffsetDateTime(),
                        reservableDate && lab.getStatus() == LabStatus.ACTIVE
                                && slotStart.atZone(TimeConfig.BUSINESS_ZONE).toOffsetDateTime().isAfter(now)
                                && !slotStart.atZone(TimeConfig.BUSINESS_ZONE).toOffsetDateTime()
                                .isAfter(now.plusHours(7 * 24L))
                                && !occupied.contains(toUtc(slotStart))))
                .toList();
        return new LabAvailabilityVO(lab.getId(), queryDTO.date(), lab.getOpenTime(), lab.getCloseTime(), slots);
    }

    /**
     * 查询实验室，不存在时返回统一实验室不存在错误。
     *
     * @param labId 实验室业务编号
     * @return 当前实验室资料
     */
    private Lab requireLab(String labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.LAB_NOT_FOUND, "实验室不存在");
        }
        return lab;
    }

    /**
     * 服务层再次确认管理员身份，避免内部调用绕过 Controller 门禁。
     */
    private void requireAdmin() {
        if (CurrentUser.requireRole() != UserRole.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "仅管理员可以修改实验室");
        }
    }

    /**
     * 将实验室实体映射为不含内部版本字段的接口摘要。
     *
     * @param lab 实验室实体
     * @return 客户端展示对象
     */
    private LabVO toVO(Lab lab) {
        return new LabVO(lab.getId(), lab.getName(), lab.getCapacity(), lab.getEquipmentDescription(),
                lab.getOpenTime(), lab.getCloseTime(), lab.getStatus());
    }

    /**
     * 返回两个时间中较晚者；空查询边界不收紧实验室开放时间。
     *
     * @param openTime 实验室开放时间
     * @param requestedFrom 可选请求开始时间
     * @return 实际查询开始时间
     */
    private LocalTime latest(LocalTime openTime, LocalTime requestedFrom) {
        return requestedFrom == null || requestedFrom.isBefore(openTime) ? openTime : requestedFrom;
    }

    /**
     * 返回两个时间中较早者；空查询边界不收紧实验室关闭时间。
     *
     * @param closeTime 实验室关闭时间
     * @param requestedTo 可选请求结束时间
     * @return 实际查询结束时间
     */
    private LocalTime earliest(LocalTime closeTime, LocalTime requestedTo) {
        return requestedTo == null || requestedTo.isAfter(closeTime) ? closeTime : requestedTo;
    }

    /**
     * 将任意查询开始时间向上对齐到完整整点，避免返回不完整时隙。
     *
     * @param time 查询开始时间
     * @return 不早于输入时间的整点
     */
    private LocalTime roundUpToHour(LocalTime time) {
        if (time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0) {
            return time;
        }
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }

    /**
     * 将上海本地时刻转换为数据库查询使用的 UTC LocalDateTime。
     *
     * @param localTime 上海本地时间
     * @return UTC 时间
     */
    private LocalDateTime toUtc(LocalDateTime localTime) {
        return LocalDateTime.ofInstant(localTime.atZone(TimeConfig.BUSINESS_ZONE).toInstant(), UTC);
    }
}
