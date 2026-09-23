package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.RepairTicketUpdateDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.service.RepairTicketService;
import io.github.decadedx.springaiagent.vo.RepairTicketPageVO;
import io.github.decadedx.springaiagent.vo.RepairTicketVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供管理员对报修工单的受控状态流转接口。
 */
@RestController
@RequestMapping("/api/admin/repair-tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRepairTicketController {

    /** 管理员工单处理领域服务。 */
    private final RepairTicketService repairTicketService;

    /**
     * 创建管理员工单控制器。
     *
     * @param repairTicketService 报修工单领域服务
     */
    public AdminRepairTicketController(RepairTicketService repairTicketService) {
        this.repairTicketService = repairTicketService;
    }

    /**
     * 分页返回管理员可处理的工单，可按当前状态筛选。
     *
     * @param queryDTO 状态和分页条件
     * @return 管理员可见的工单分页结果
     */
    @GetMapping
    public Result<RepairTicketPageVO> findPage(@Valid @ModelAttribute RepairTicketQueryDTO queryDTO) {
        return Result.success(repairTicketService.findAll(queryDTO));
    }

    /**
     * 将工单流转至唯一允许的下一状态，并记录本次处理说明。
     *
     * @param ticketId 待处理工单主键
     * @param updateDTO 目标状态和处理说明
     * @return 更新后的工单
     */
    @PatchMapping("/{ticketId}")
    public Result<RepairTicketVO> process(@PathVariable Long ticketId,
                                          @Valid @RequestBody RepairTicketUpdateDTO updateDTO) {
        return Result.success(repairTicketService.process(ticketId, updateDTO));
    }
}
