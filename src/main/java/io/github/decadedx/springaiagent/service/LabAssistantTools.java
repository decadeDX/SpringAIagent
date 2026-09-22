package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.dto.KnowledgeQuestionDTO;
import io.github.decadedx.springaiagent.dto.LabAvailabilityQueryDTO;
import io.github.decadedx.springaiagent.dto.LabQueryDTO;
import io.github.decadedx.springaiagent.dto.RepairDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.ReservationQueryDTO;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import io.github.decadedx.springaiagent.vo.LabAvailabilityVO;
import io.github.decadedx.springaiagent.vo.LabPageVO;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;
import io.github.decadedx.springaiagent.vo.RepairTicketPageVO;
import io.github.decadedx.springaiagent.vo.ReservationPageVO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 暴露给模型的唯一业务工具集合；所有身份均从安全上下文读取，写操作只能创建临时草案。
 */
@Component
public class LabAssistantTools {

    /** 知识库问答服务。 */
    private final RagService ragService;

    /** 实验室实时查询服务。 */
    private final LabService labService;

    /** 预约领域服务。 */
    private final ReservationService reservationService;

    /** 报修领域服务。 */
    private final RepairTicketService repairTicketService;

    /** 动作草案服务。 */
    private final ActionDraftService actionDraftService;

    /** 当前模型调用的额度、引用和草案上下文。 */
    private final AgentCallContext agentCallContext;

    /** 脱敏工具调用审计服务。 */
    private final AgentTraceService agentTraceService;

    /**
     * 创建受控工具集合。
     *
     * @param ragService 知识问答服务
     * @param labService 实验室服务
     * @param reservationService 预约服务
     * @param repairTicketService 报修服务
     * @param actionDraftService 草案服务
     * @param agentCallContext Agent 上下文
     * @param agentTraceService 审计服务
     */
    public LabAssistantTools(RagService ragService, LabService labService, ReservationService reservationService,
                             RepairTicketService repairTicketService, ActionDraftService actionDraftService,
                             AgentCallContext agentCallContext, AgentTraceService agentTraceService) {
        this.ragService = ragService;
        this.labService = labService;
        this.reservationService = reservationService;
        this.repairTicketService = repairTicketService;
        this.actionDraftService = actionDraftService;
        this.agentCallContext = agentCallContext;
        this.agentTraceService = agentTraceService;
    }

    /**
     * 检索已发布知识库，并将真实引用带回本轮响应。
     *
     * @param question 用户问题
     * @return 带引用的知识回答
     */
    @Tool(description = "检索已发布的实验室制度和安全资料。实时空位和用户个人记录不要使用此工具。")
    public RagAnswerVO searchKnowledge(@ToolParam(description = "需要查询的制度或安全问题") String question) {
        return traced("searchKnowledge", "{\"question\":\"已脱敏\"}", () -> {
            RagAnswerVO answer = ragService.ask(new KnowledgeQuestionDTO(question));
            agentCallContext.addCitations(answer.citations());
            return answer;
        });
    }

    /**
     * 按设备和容量筛选实验室，不承诺最终预约一定成功。
     *
     * @param equipment 设备关键词，可为空
     * @param participantCount 参与人数，可为空
     * @return 候选实验室
     */
    @Tool(description = "按设备需求和人数查询候选实验室；结果仅用于选择候选，不代表时段仍可预约。")
    public LabPageVO searchAvailableLabs(@ToolParam(description = "设备关键词，如 GPU；没有要求时传空字符串") String equipment,
                                         @ToolParam(description = "参与人数；未知时传 0") Integer participantCount) {
        return traced("searchAvailableLabs", "{\"equipment\":\"已脱敏\",\"participantCount\":\"已脱敏\"}",
                () -> labService.search(new LabQueryDTO(null, equipment,
                        participantCount == null || participantCount <= 0 ? null : participantCount, 1, 20)));
    }

    /**
     * 查询某实验室指定日期的实际可用整点时隙。
     *
     * @param labId 实验室编号
     * @param date ISO 日期
     * @return 可用性结果
     */
    @Tool(description = "查询指定实验室某一天的实时空闲整点时隙。date 必须是服务端展示的 ISO 日期。")
    public LabAvailabilityVO getLabAvailability(@ToolParam(description = "实验室编号") String labId,
                                                @ToolParam(description = "ISO 日期，格式 yyyy-MM-dd") String date) {
        return traced("getLabAvailability", "{\"labId\":\"已脱敏\",\"date\":\"已脱敏\"}",
                () -> labService.availability(labId, new LabAvailabilityQueryDTO(parseDate(date), null, null)));
    }

    /**
     * 查询当前用户自己的预约，不能指定其他用户。
     *
     * @return 当前用户预约分页结果
     */
    @Tool(description = "查询当前用户自己的预约记录，不能查询其他人的预约。")
    public ReservationPageVO getMyReservations() {
        return traced("getMyReservations", "{}", () -> reservationService.findMine(new ReservationQueryDTO(null, null, null, 1, 20)));
    }

    /**
     * 通过已有领域校验创建预约草案，不写入预约或时隙表。
     *
     * @param labId 实验室编号
     * @param startTime ISO 带 +08:00 偏移的开始时间
     * @param endTime ISO 带 +08:00 偏移的结束时间
     * @param participantCount 参与人数
     * @return 待确认草案
     */
    @Tool(description = "校验完整预约条件并生成待确认草案。只有实验室、开始时间、结束时间和人数全部明确时才调用；绝不能确认或写入预约。")
    public ActionDraftVO prepareReservation(@ToolParam(description = "实验室编号") String labId,
                                            @ToolParam(description = "ISO 开始时间，必须带 +08:00") String startTime,
                                            @ToolParam(description = "ISO 结束时间，必须带 +08:00") String endTime,
                                            @ToolParam(description = "参与人数") Integer participantCount) {
        return traced("prepareReservation", "{\"labId\":\"已脱敏\",\"time\":\"已脱敏\",\"participantCount\":\"已脱敏\"}", () -> {
            var payload = reservationService.prepareCreate(new ReservationDraftCreateDTO(labId, parseOffsetTime(startTime),
                    parseOffsetTime(endTime), participantCount));
            ActionDraftVO draft = actionDraftService.create(ActionType.CREATE_RESERVATION,
                    currentSessionId(), payload, List.of("确认时会重新校验时段、培训资格和实验室占用。"));
            agentCallContext.setDraft(draft);
            return draft;
        });
    }

    /**
     * 为当前用户的预约生成取消草案，不改变预约状态。
     *
     * @param reservationId 当前用户预约主键
     * @return 待确认草案
     */
    @Tool(description = "为当前用户自己的预约生成取消草案；调用前应先查询预约并在有多个候选时要求用户选择。绝不能直接取消。")
    public ActionDraftVO prepareCancellation(@ToolParam(description = "当前用户要取消的预约主键") Long reservationId) {
        return traced("prepareCancellation", "{\"reservationId\":\"已脱敏\"}", () -> {
            var payload = reservationService.prepareCancel(reservationId);
            ActionDraftVO draft = actionDraftService.create(ActionType.CANCEL_RESERVATION, currentSessionId(), payload,
                    List.of("确认时会重新校验预约归属和提前取消时间。"));
            agentCallContext.setDraft(draft);
            return draft;
        });
    }

    /**
     * 为当前用户的报修生成草案，高风险提示由领域服务强制给出。
     *
     * @param labId 实验室编号
     * @param equipmentInfo 设备信息
     * @param description 故障说明
     * @return 待确认草案
     */
    @Tool(description = "为当前用户准备报修草案；必须包含实验室、设备和故障说明。焦糊味、冒烟或漏电时必须展示安全提示，绝不能直接提交。")
    public ActionDraftVO prepareRepairTicket(@ToolParam(description = "实验室编号") String labId,
                                             @ToolParam(description = "设备名称或资产编号") String equipmentInfo,
                                             @ToolParam(description = "故障说明") String description) {
        return traced("prepareRepairTicket", "{\"labId\":\"已脱敏\",\"equipment\":\"已脱敏\"}", () -> {
            var payload = repairTicketService.prepareCreate(new RepairDraftCreateDTO(labId, equipmentInfo, description, null));
            ActionDraftVO draft = actionDraftService.create(ActionType.CREATE_REPAIR_TICKET, currentSessionId(), payload,
                    payload.safetyNotices());
            agentCallContext.setDraft(draft);
            return draft;
        });
    }

    /**
     * 查询当前用户自己的报修工单，不能指定其他用户。
     *
     * @return 当前用户工单分页结果
     */
    @Tool(description = "查询当前用户自己的报修工单，不能查询其他人的工单。")
    public RepairTicketPageVO getMyRepairTickets() {
        return traced("getMyRepairTickets", "{}", () -> repairTicketService.findMine(new RepairTicketQueryDTO(null, 1, 20)));
    }

    /**
     * 在工具调用前后记录审计，并保证第九次调用不会访问任何领域服务。
     *
     * @param toolName 工具名称
     * @param redactedArguments 已脱敏参数
     * @param operation 实际业务调用
     * @param <T> 工具返回类型
     * @return 工具结果
     */
    private <T> T traced(String toolName, String redactedArguments, Callable<T> operation) {
        agentCallContext.acquireToolCall();
        long startedAt = System.nanoTime();
        try {
            T result = operation.call();
            agentTraceService.record(currentSessionId(), toolName, redactedArguments,
                    result == null ? "无结果" : result.getClass().getSimpleName(), elapsedMillis(startedAt), null);
            return result;
        } catch (BusinessException exception) {
            agentTraceService.record(currentSessionId(), toolName, redactedArguments, "业务失败",
                    elapsedMillis(startedAt), exception.getCode().name());
            throw exception;
        } catch (Exception exception) {
            agentTraceService.record(currentSessionId(), toolName, redactedArguments, "工具失败",
                    elapsedMillis(startedAt), ApiCode.INTERNAL_ERROR.name());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, ApiCode.DEPENDENCY_UNAVAILABLE,
                    "助手工具暂不可用，请稍后重试");
        }
    }

    /**
     * 读取工具上下文的会话标识。
     *
     * @return 当前会话标识
     */
    private String currentSessionId() {
        return agentCallContext.sessionId();
    }

    /**
     * 解析模型传入的日期并拒绝不符合工具 schema 的文本。
     *
     * @param value ISO 日期
     * @return 日期对象
     */
    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "日期格式必须为 yyyy-MM-dd");
        }
    }

    /**
     * 解析模型传入的带偏移预约时间。
     *
     * @param value ISO 带偏移时间
     * @return 时间对象
     */
    private OffsetDateTime parseOffsetTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ApiCode.BAD_REQUEST, "预约时间必须为带 +08:00 的 ISO 时间");
        }
    }

    /**
     * 计算工具耗时。
     *
     * @param startedAt 起始纳秒
     * @return 非负毫秒
     */
    private int elapsedMillis(long startedAt) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L));
    }
}
