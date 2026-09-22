package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.service.ActionDraftService;
import io.github.decadedx.springaiagent.service.ReservationService;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 提供预约创建和取消的草案接口，真实写入必须经动作确认接口完成。
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('STUDENT')")
public class ReservationDraftController {

    /** 预约领域准备服务。 */
    private final ReservationService reservationService;

    /** 短期草案服务。 */
    private final ActionDraftService actionDraftService;

    /**
     * 创建预约草案控制器。
     *
     * @param reservationService 预约服务
     * @param actionDraftService 草案服务
     */
    public ReservationDraftController(ReservationService reservationService, ActionDraftService actionDraftService) {
        this.reservationService = reservationService;
        this.actionDraftService = actionDraftService;
    }

    /**
     * 校验预约条件并返回独立业务会话的待确认草案。
     *
     * @param createDTO 原始预约输入
     * @return 待确认草案
     */
    @PostMapping("/reservation-drafts")
    public ResponseEntity<Result<ActionDraftVO>> create(@Valid @RequestBody ReservationDraftCreateDTO createDTO) {
        ActionDraftVO draft = actionDraftService.create(ActionType.CREATE_RESERVATION, null,
                reservationService.prepareCreate(createDTO), List.of("确认时会重新校验时段、培训资格和实验室占用。"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(draft));
    }

    /**
     * 校验取消边界并返回独立业务会话的待确认草案。
     *
     * @param reservationId 当前学生的预约主键
     * @return 待确认草案
     */
    @PostMapping("/reservations/{reservationId}/cancellation-draft")
    public ResponseEntity<Result<ActionDraftVO>> cancel(@PathVariable Long reservationId) {
        ActionDraftVO draft = actionDraftService.create(ActionType.CANCEL_RESERVATION, null,
                reservationService.prepareCancel(reservationId), List.of("确认时会重新校验预约归属和取消时间。"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(draft));
    }
}
