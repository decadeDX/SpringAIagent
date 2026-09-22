package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.RepairDraftCreateDTO;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.service.ActionDraftService;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供学生报修草案接口；提交工单只能由确认接口执行。
 */
@RestController
@RequestMapping("/api/repair-drafts")
@PreAuthorize("hasRole('STUDENT')")
public class RepairDraftController {

    /** 报修领域准备服务。 */
    private final RepairTicketService repairTicketService;

    /** 草案服务。 */
    private final ActionDraftService actionDraftService;

    /**
     * 创建报修草案控制器。
     *
     * @param repairTicketService 报修服务
     * @param actionDraftService 草案服务
     */
    public RepairDraftController(RepairTicketService repairTicketService, ActionDraftService actionDraftService) {
        this.repairTicketService = repairTicketService;
        this.actionDraftService = actionDraftService;
    }

    /**
     * 校验报修内容并返回待确认草案及固定安全提示。
     *
     * @param createDTO 报修输入
     * @return 待确认草案
     */
    @PostMapping
    public ResponseEntity<Result<ActionDraftVO>> create(@Valid @RequestBody RepairDraftCreateDTO createDTO) {
        var payload = repairTicketService.prepareCreate(createDTO);
        ActionDraftVO draft = actionDraftService.create(ActionType.CREATE_REPAIR_TICKET, null, payload,
                payload.safetyNotices());
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(draft));
    }
}
