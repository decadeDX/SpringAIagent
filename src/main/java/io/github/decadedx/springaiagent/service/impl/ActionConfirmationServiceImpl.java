package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.common.ApplicationMetrics;
import io.github.decadedx.springaiagent.dto.ActionConfirmDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketCreatePayload;
import io.github.decadedx.springaiagent.dto.ReservationCancelPayload;
import io.github.decadedx.springaiagent.dto.ReservationCreatePayload;
import io.github.decadedx.springaiagent.entity.ActionExecution;
import io.github.decadedx.springaiagent.enums.ActionExecutionStatus;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.ActionExecutionMapper;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.ActionConfirmationService;
import io.github.decadedx.springaiagent.service.ActionDraftService;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.service.ReservationService;
import io.github.decadedx.springaiagent.service.StoredActionDraft;
import io.github.decadedx.springaiagent.vo.ActionExecutionVO;
import io.github.decadedx.springaiagent.vo.RepairTicketVO;
import io.github.decadedx.springaiagent.vo.ReservationVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 以 MySQL 为最终幂等事实执行草案，Redis 仅在首次确认时提供可信临时载荷。
 */
@Service
public class ActionConfirmationServiceImpl implements ActionConfirmationService {

    /** 动作结果中必须以字符串公开的持久化主键字段。 */
    private static final Set<String> RESULT_ID_FIELDS = Set.of("businessId", "reservationId", "ticketId");

    /** 动作执行记录访问入口。 */
    private final ActionExecutionMapper actionExecutionMapper;

    /** Redis 草案访问服务。 */
    private final ActionDraftService actionDraftService;

    /** 预约领域确认服务。 */
    private final ReservationService reservationService;

    /** 报修领域确认服务。 */
    private final RepairTicketService repairTicketService;

    /** 可信 JSON 载荷转换器。 */
    private final ObjectMapper objectMapper;

    /** 成功写入与领域事务模板。 */
    private final TransactionTemplate transactionTemplate;

    /** 失败状态独立记录事务模板。 */
    private final TransactionTemplate failureTransactionTemplate;

    /** 动作幂等重放指标。 */
    private final ApplicationMetrics applicationMetrics;

    /**
     * 创建动作确认服务。
     *
     * @param actionExecutionMapper 幂等记录 Mapper
     * @param actionDraftService 草案服务
     * @param reservationService 预约服务
     * @param repairTicketService 报修服务
     * @param objectMapper JSON 转换器
     * @param transactionManager 数据库事务管理器
     * @param applicationMetrics 动作执行指标
     */
    public ActionConfirmationServiceImpl(ActionExecutionMapper actionExecutionMapper,
                                         ActionDraftService actionDraftService,
                                         ReservationService reservationService,
                                         RepairTicketService repairTicketService,
                                         ObjectMapper objectMapper,
                                         PlatformTransactionManager transactionManager,
                                         ApplicationMetrics applicationMetrics) {
        this.actionExecutionMapper = actionExecutionMapper;
        this.actionDraftService = actionDraftService;
        this.reservationService = reservationService;
        this.repairTicketService = repairTicketService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.failureTransactionTemplate = new TransactionTemplate(transactionManager);
        this.failureTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.applicationMetrics = applicationMetrics;
    }

    /** {@inheritDoc} */
    @Override
    public ActionExecutionVO confirm(String actionId, ActionConfirmDTO confirmDTO) {
        if (actionId == null || actionId.isBlank()) {
            throw expired();
        }
        Long userId = CurrentUser.requireId();
        ActionExecution persisted = actionExecutionMapper.selectById(actionId);
        if (persisted != null) {
            return replayOrFailure(persisted, userId);
        }
        StoredActionDraft draft = actionDraftService.find(actionId);
        validateDraft(draft, userId, confirmDTO);
        ActionExecutionVO result;
        try {
            result = transactionTemplate.execute(status -> executeNew(draft, userId));
        } catch (BusinessException exception) {
            recordFailure(draft, exception);
            throw exception;
        }
        try {
            actionDraftService.delete(actionId);
        } catch (BusinessException ignored) {
        }
        return result;
    }

    /**
     * 在同一事务内创建执行记录、执行领域写入并保存成功结果。
     *
     * @param draft 已验证草案
     * @param userId 当前用户
     * @return 执行结果
     */
    private ActionExecutionVO executeNew(StoredActionDraft draft, Long userId) {
        ActionExecution existing = actionExecutionMapper.selectByActionIdForUpdate(draft.actionId());
        if (existing != null) {
            return replayOrFailure(existing, userId);
        }
        ActionExecution execution = new ActionExecution();
        execution.setActionId(draft.actionId());
        execution.setUserId(userId);
        execution.setSessionId(draft.sessionId());
        execution.setActionType(draft.actionType());
        execution.setExecutionStatus(ActionExecutionStatus.EXECUTING);
        execution.setPayload(writeJson(draft.payload()));
        try {
            actionExecutionMapper.insert(execution);
        } catch (DuplicateKeyException exception) {
            ActionExecution competing = actionExecutionMapper.selectByActionIdForUpdate(draft.actionId());
            if (competing != null) {
                return replayOrFailure(competing, userId);
            }
            throw exception;
        }

        Map<String, Object> result = executeDomain(draft);
        execution.setBusinessId(Long.parseLong((String) result.get("businessId")));
        execution.setResultSummary(writeJson(result));
        execution.setExecutionStatus(ActionExecutionStatus.SUCCEEDED);
        actionExecutionMapper.updateById(execution);
        return new ActionExecutionVO(execution.getActionId(), execution.getActionType(),
                ActionExecutionStatus.SUCCEEDED, false, result);
    }

    /**
     * 调用现有领域服务，确认阶段由其重新读取并校验实时规则。
     *
     * @param draft 已验证草案
     * @return 可持久化的最小成功结果
     */
    private Map<String, Object> executeDomain(StoredActionDraft draft) {
        return switch (draft.actionType()) {
            case CREATE_RESERVATION -> reservationResult(reservationService.confirmCreate(
                    objectMapper.convertValue(draft.payload(), ReservationCreatePayload.class)));
            case CANCEL_RESERVATION -> reservationResult(reservationService.confirmCancel(
                    objectMapper.convertValue(draft.payload(), ReservationCancelPayload.class)));
            case CREATE_REPAIR_TICKET -> repairResult(repairTicketService.confirmCreate(
                    objectMapper.convertValue(draft.payload(), RepairTicketCreatePayload.class)));
        };
    }

    /**
     * 将预约结果压缩为幂等响应需要的字段。
     *
     * @param reservation 已确认或取消的预约
     * @return 结果 JSON 对象
     */
    private Map<String, Object> reservationResult(ReservationVO reservation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessId", reservation.id().toString());
        result.put("reservationId", reservation.id().toString());
        result.put("reservationNo", reservation.reservationNo());
        result.put("status", reservation.status().name());
        return result;
    }

    /**
     * 将工单结果压缩为幂等响应需要的字段。
     *
     * @param ticket 已创建工单
     * @return 结果 JSON 对象
     */
    private Map<String, Object> repairResult(RepairTicketVO ticket) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("businessId", ticket.id().toString());
        result.put("ticketId", ticket.id().toString());
        result.put("ticketNo", ticket.ticketNo());
        result.put("status", ticket.status().name());
        return result;
    }

    /**
     * 处理已有执行记录，防止 actionId 被其他用户探测。
     *
     * @param execution 已持久化动作
     * @param userId 当前用户
     * @return 成功重放结果
     */
    private ActionExecutionVO replayOrFailure(ActionExecution execution, Long userId) {
        if (!userId.equals(execution.getUserId())) {
            throw expired();
        }
        if (execution.getExecutionStatus() == ActionExecutionStatus.SUCCEEDED) {
            applicationMetrics.actionIdempotentReplay();
            return new ActionExecutionVO(execution.getActionId(), execution.getActionType(),
                    ActionExecutionStatus.SUCCEEDED, true, readMap(execution.getResultSummary()));
        }
        if (execution.getExecutionStatus() == ActionExecutionStatus.FAILED) {
            throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT,
                    "该草案此前确认失败，请重新生成草案后再试");
        }
        throw new BusinessException(HttpStatus.CONFLICT, ApiCode.BUSINESS_CONFLICT, "该草案正在确认，请稍后重试");
    }

    /**
     * 校验 Redis 草案绑定的用户、会话、状态和到期时间。
     *
     * @param draft Redis 草案
     * @param userId 当前用户
     * @param confirmDTO 客户端确认请求
     */
    private void validateDraft(StoredActionDraft draft, Long userId, ActionConfirmDTO confirmDTO) {
        if (draft == null || confirmDTO == null || !userId.equals(draft.userId())
                || !draft.sessionId().equals(confirmDTO.sessionId())) {
            throw expired();
        }
    }

    /**
     * 在领域事务失败后写入失败记录，避免保留任何部分业务写入。
     *
     * @param draft 原草案
     * @param exception 领域失败
     */
    private void recordFailure(StoredActionDraft draft, BusinessException exception) {
        failureTransactionTemplate.executeWithoutResult(status -> {
            ActionExecution execution = actionExecutionMapper.selectById(draft.actionId());
            if (execution == null) {
                execution = new ActionExecution();
                execution.setActionId(draft.actionId());
                execution.setUserId(draft.userId());
                execution.setSessionId(draft.sessionId());
                execution.setActionType(draft.actionType());
                execution.setPayload(writeJson(draft.payload()));
                execution.setExecutionStatus(ActionExecutionStatus.FAILED);
                execution.setFailureCode(exception.getCode().name());
                actionExecutionMapper.insert(execution);
            }
        });
    }

    /**
     * 写入 JSON；受控 DTO 无法序列化时属于服务端错误。
     *
     * @param value 待写入值
     * @return JSON 文本
     */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("动作载荷无法序列化", exception);
        }
    }

    /**
     * 读取已持久化成功结果。
     *
     * @param json 结果 JSON
     * @return 已将历史数值主键规范化为字符串的不可变结果对象
     */
    private Map<String, Object> readMap(String json) {
        try {
            Map<String, Object> result = new LinkedHashMap<>(objectMapper.readValue(json, Map.class));
            RESULT_ID_FIELDS.forEach(field -> {
                Object value = result.get(field);
                if (value instanceof Number number) {
                    result.put(field, number.toString());
                }
            });
            return Map.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalStateException("动作结果无法读取", exception);
        }
    }

    /**
     * 返回不可安全执行的草案错误。
     *
     * @return 过期或无权草案异常
     */
    private BusinessException expired() {
        return new BusinessException(HttpStatus.CONFLICT, ApiCode.ACTION_DRAFT_EXPIRED, "草案已过期或不可用，请重新生成");
    }
}
