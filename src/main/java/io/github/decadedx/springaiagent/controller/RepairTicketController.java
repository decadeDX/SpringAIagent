package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.vo.RepairTicketPageVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供学生本人报修工单的查询接口；草案与确认端点由模块 05 统一提供。
 */
@RestController
@RequestMapping("/api/repair-tickets")
@PreAuthorize("hasRole('STUDENT')")
public class RepairTicketController {

    /** 当前学生报修工单领域服务。 */
    private final RepairTicketService repairTicketService;

    /**
     * 创建本人报修工单查询控制器。
     *
     * @param repairTicketService 报修工单领域服务
     */
    public RepairTicketController(RepairTicketService repairTicketService) {
        this.repairTicketService = repairTicketService;
    }

    /**
     * 分页查询当前学生自己的工单，服务层固定从认证上下文读取身份。
     *
     * @param queryDTO 状态和分页条件
     * @return 本人工单分页结果
     */
    @GetMapping("/me")
    public Result<RepairTicketPageVO> findMine(@Valid @ModelAttribute RepairTicketQueryDTO queryDTO) {
        return Result.success(repairTicketService.findMine(queryDTO));
    }
}
