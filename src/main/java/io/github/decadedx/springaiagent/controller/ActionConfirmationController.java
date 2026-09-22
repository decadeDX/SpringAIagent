package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.ActionConfirmDTO;
import io.github.decadedx.springaiagent.service.ActionConfirmationService;
import io.github.decadedx.springaiagent.vo.ActionExecutionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供唯一的草案确认入口，禁止 Controller 直接调用预约或报修写入服务。
 */
@RestController
@RequestMapping("/api/actions")
public class ActionConfirmationController {

    /** 动作确认服务。 */
    private final ActionConfirmationService actionConfirmationService;

    /**
     * 创建确认控制器。
     *
     * @param actionConfirmationService 动作确认服务
     */
    public ActionConfirmationController(ActionConfirmationService actionConfirmationService) {
        this.actionConfirmationService = actionConfirmationService;
    }

    /**
     * 确认或幂等重放一个由当前用户创建的草案。
     *
     * @param actionId 草案动作标识
     * @param confirmDTO 草案所属会话
     * @return 首次确认或成功重放结果
     */
    @PostMapping("/{actionId}/confirm")
    public Result<ActionExecutionVO> confirm(@PathVariable String actionId, @Valid @RequestBody ActionConfirmDTO confirmDTO) {
        return Result.success(actionConfirmationService.confirm(actionId, confirmDTO));
    }
}
