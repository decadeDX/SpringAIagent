package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.AgentTraceQueryDTO;
import io.github.decadedx.springaiagent.service.AgentTraceService;
import io.github.decadedx.springaiagent.vo.AgentTracePageVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员查看 Agent 工具审计摘要的只读接口。
 */
@RestController
@RequestMapping("/api/admin/agent-traces")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentTraceController {

    /** 审计服务。 */
    private final AgentTraceService agentTraceService;

    /**
     * 创建审计查询控制器。
     *
     * @param agentTraceService 审计服务
     */
    public AdminAgentTraceController(AgentTraceService agentTraceService) {
        this.agentTraceService = agentTraceService;
    }

    /**
     * 按请求或会话分页查询脱敏工具记录。
     *
     * @param queryDTO 查询条件
     * @return 审计分页结果
     */
    @GetMapping
    public Result<AgentTracePageVO> list(@Valid @ModelAttribute AgentTraceQueryDTO queryDTO) {
        return Result.success(agentTraceService.findForAdmin(queryDTO));
    }
}
