package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.RepairDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketCreatePayload;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketUpdateDTO;
import io.github.decadedx.springaiagent.entity.Lab;
import io.github.decadedx.springaiagent.entity.RepairTicket;
import io.github.decadedx.springaiagent.entity.SysUser;
import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.LabMapper;
import io.github.decadedx.springaiagent.mapper.RepairTicketMapper;
import io.github.decadedx.springaiagent.mapper.SysUserMapper;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.vo.RepairTicketPageVO;
import io.github.decadedx.springaiagent.vo.RepairTicketVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 实现报修工单的校验、确认创建和管理员受控流转；报修不会修改实验室状态。
 */
@Service
public class RepairTicketServiceImpl implements RepairTicketService {

    /** 数据库时间字段使用的 UTC 偏移。 */
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    /** 描述命中后必须升级为高风险工单的关键词。 */
    private static final List<String> HIGH_RISK_KEYWORDS = List.of("冒烟", "漏电", "焦糊味");

    /** 高风险工单对学生返回且不可被模型替换的固定安全提示。 */
    private static final String HIGH_RISK_NOTICE = "请立即停止使用设备，避免自行维修，并联系管理员。";

    /** 实验室资料访问入口，用于确认实验室仍存在。 */
    private final LabMapper labMapper;

    /** 工单聚合根访问入口。 */
    private final RepairTicketMapper repairTicketMapper;

    /** 用户访问入口，用于确认认证身份对应的账号仍存在。 */
    private final SysUserMapper sysUserMapper;

    /**
     * 创建报修工单领域服务。
     *
     * @param labMapper 实验室 Mapper
     * @param repairTicketMapper 工单 Mapper
     * @param sysUserMapper 用户 Mapper
     */
    public RepairTicketServiceImpl(LabMapper labMapper, RepairTicketMapper repairTicketMapper,
                                   SysUserMapper sysUserMapper) {
        this.labMapper = labMapper;
        this.repairTicketMapper = repairTicketMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepairTicketCreatePayload prepareCreate(RepairDraftCreateDTO createDTO) {
        requireStudent();
        validateCreateInput(createDTO);
        Lab lab = requireLab(createDTO.labId());
        boolean safetyRisk = isSafetyRisk(createDTO.description(), createDTO.safetyRisk());
        return new RepairTicketCreatePayload(lab.getId(), lab.getName(), createDTO.equipmentInfo(),
                createDTO.description(), safetyRisk, safetyRisk ? List.of(HIGH_RISK_NOTICE) : List.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairTicketVO confirmCreate(RepairTicketCreatePayload payload) {
        Long userId = requireStudent();
        validatePayload(payload);
        requireLab(payload.labId());

        RepairTicket ticket = new RepairTicket();
        ticket.setUserId(userId);
        ticket.setLabId(payload.labId());
        ticket.setEquipmentInfo(payload.equipmentInfo());
        ticket.setDescription(payload.description());
        ticket.setSafetyRisk(isSafetyRisk(payload.description(), payload.safetyRisk()));
        ticket.setStatus(RepairTicketStatus.SUBMITTED);
        repairTicketMapper.insert(ticket);
        return toVO(repairTicketMapper.selectById(ticket.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepairTicketPageVO findMine(RepairTicketQueryDTO queryDTO) {
        Long userId = requireStudent();
        RepairTicketQueryDTO criteria = queryDTO == null
                ? new RepairTicketQueryDTO(null, null, null) : queryDTO;
        int page = criteria.page() == null ? 1 : criteria.page();
        int size = criteria.size() == null ? 20 : criteria.size();
        long total = repairTicketMapper.countMine(userId, criteria.status());
        List<RepairTicketVO> items = repairTicketMapper.selectMine(userId, criteria.status(),
                        (long) (page - 1) * size, size)
                .stream()
                .map(this::toVO)
                .toList();
        return new RepairTicketPageVO(items, page, size, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RepairTicketPageVO findAll(RepairTicketQueryDTO queryDTO) {
        requireAdmin();
        RepairTicketQueryDTO criteria = queryDTO == null
                ? new RepairTicketQueryDTO(null, null, null) : queryDTO;
        int page = criteria.page() == null ? 1 : criteria.page();
        int size = criteria.size() == null ? 20 : criteria.size();
        long total = repairTicketMapper.countAll(criteria.status());
        List<RepairTicketVO> items = repairTicketMapper.selectAll(criteria.status(), (long) (page - 1) * size, size)
                .stream()
                .map(this::toVO)
                .toList();
        return new RepairTicketPageVO(items, page, size, total);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairTicketVO process(Long ticketId, RepairTicketUpdateDTO updateDTO) {
        Long adminId = requireAdmin();
        if (ticketId == null || updateDTO == null || updateDTO.status() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "工单处理参数不完整");
        }
        if (!StringUtils.hasText(updateDTO.resolutionNote())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, ApiCode.RESOLUTION_NOTE_REQUIRED,
                    "处理说明不能为空");
        }

        RepairTicket ticket = repairTicketMapper.selectByIdForUpdate(ticketId);
        if (ticket == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.REPAIR_TICKET_NOT_FOUND, "工单不存在");
        }
        if (!isAdjacentTransition(ticket.getStatus(), updateDTO.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.INVALID_TICKET_TRANSITION,
                    "工单状态只能按相邻步骤流转");
        }
        if (repairTicketMapper.updateProcess(ticketId, updateDTO.status(), updateDTO.resolutionNote(), adminId,
                ticket.getVersion()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "工单已被其他管理员处理，请刷新后重试");
        }
        ticket.setStatus(updateDTO.status());
        ticket.setResolutionNote(updateDTO.resolutionNote());
        ticket.setProcessedBy(adminId);
        ticket.setVersion(ticket.getVersion() + 1);
        return toVO(repairTicketMapper.selectById(ticketId));
    }

    /**
     * 检查创建阶段必填字段，缺失字段必须使用报修专属稳定错误码。
     *
     * @param createDTO 原始草案输入
     */
    private void validateCreateInput(RepairDraftCreateDTO createDTO) {
        if (createDTO == null || !StringUtils.hasText(createDTO.labId())
                || !StringUtils.hasText(createDTO.equipmentInfo()) || !StringUtils.hasText(createDTO.description())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.REPAIR_REQUIRED_FIELD_MISSING,
                    "实验室、设备信息和故障描述不能为空");
        }
    }

    /**
     * 重新验证恢复的草案载荷，避免确认阶段信任过期或被篡改的缓存数据。
     *
     * @param payload 模块 05 恢复的确认载荷
     */
    private void validatePayload(RepairTicketCreatePayload payload) {
        if (payload == null || !StringUtils.hasText(payload.labId()) || !StringUtils.hasText(payload.equipmentInfo())
                || !StringUtils.hasText(payload.description())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.REPAIR_REQUIRED_FIELD_MISSING,
                    "报修草案缺少必要字段");
        }
    }

    /**
     * 获取存在的实验室；维护状态不会阻止学生提交故障报修。
     *
     * @param labId 实验室业务编号
     * @return 已存在的实验室资料
     */
    private Lab requireLab(String labId) {
        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ApiCode.LAB_NOT_FOUND, "实验室不存在");
        }
        return lab;
    }

    /**
     * 根据用户主动标记和描述关键词得出不可降低的高风险结果。
     *
     * @param description 故障现象说明
     * @param requestedRisk 学生主动风险标记
     * @return 服务端最终风险标记
     */
    private boolean isSafetyRisk(String description, Boolean requestedRisk) {
        return Boolean.TRUE.equals(requestedRisk)
                || HIGH_RISK_KEYWORDS.stream().anyMatch(description::contains);
    }

    /**
     * 检查状态是否为唯一允许的下一步，禁止跳跃、回退和重复处理。
     *
     * @param currentStatus 当前状态
     * @param targetStatus 管理员请求的目标状态
     * @return 状态相邻时为 true
     */
    private boolean isAdjacentTransition(RepairTicketStatus currentStatus, RepairTicketStatus targetStatus) {
        return (currentStatus == RepairTicketStatus.SUBMITTED && targetStatus == RepairTicketStatus.PROCESSING)
                || (currentStatus == RepairTicketStatus.PROCESSING && targetStatus == RepairTicketStatus.RESOLVED);
    }

    /**
     * 从认证上下文取得学生身份，并确认其对应账号尚未被删除。
     *
     * @return 当前学生主键
     */
    private Long requireStudent() {
        if (CurrentUser.requireRole() != UserRole.STUDENT) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "仅学生可以提交和查询报修");
        }
        Long userId = CurrentUser.requireId();
        requireExistingUser(userId);
        return userId;
    }

    /**
     * 从认证上下文取得管理员身份，并确认其对应账号尚未被删除。
     *
     * @return 当前管理员主键
     */
    private Long requireAdmin() {
        if (CurrentUser.requireRole() != UserRole.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, ApiCode.FORBIDDEN, "仅管理员可以处理工单");
        }
        Long userId = CurrentUser.requireId();
        requireExistingUser(userId);
        return userId;
    }

    /**
     * 校验 JWT 身份对应的账号仍存在，删除账号后的令牌视为失效。
     *
     * @param userId 当前认证用户主键
     */
    private void requireExistingUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, ApiCode.UNAUTHENTICATED, "请先登录");
        }
    }

    /**
     * 将数据库 UTC 时间字段转换为接口使用的上海偏移时间。
     *
     * @param ticket 工单实体
     * @return 不携带用户内部身份的工单响应
     */
    private RepairTicketVO toVO(RepairTicket ticket) {
        return new RepairTicketVO(ticket.getId(), "RPT-" + ticket.getId(), ticket.getLabId(),
                ticket.getEquipmentInfo(), ticket.getDescription(), Boolean.TRUE.equals(ticket.getSafetyRisk()),
                ticket.getStatus(), ticket.getResolutionNote(), toShanghai(ticket.getCreatedAt()),
                toShanghai(ticket.getUpdatedAt()));
    }

    /**
     * 将 UTC 的数据库时间转换为 Asia/Shanghai 偏移表示；刚插入未回填时间时保持为空。
     *
     * @param value 数据库 UTC 时间
     * @return 上海偏移时间或空值
     */
    private OffsetDateTime toShanghai(LocalDateTime value) {
        return value == null ? null : value.atOffset(UTC).atZoneSameInstant(TimeConfig.BUSINESS_ZONE).toOffsetDateTime();
    }
}
