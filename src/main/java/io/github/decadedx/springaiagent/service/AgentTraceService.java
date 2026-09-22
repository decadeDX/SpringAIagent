package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.AgentTraceQueryDTO;
import io.github.decadedx.springaiagent.vo.AgentTracePageVO;

/**
 * 记录 Agent 工具调用的最小可观测信息。
 */
public interface AgentTraceService {

    /**
     * 写入一条已经脱敏的工具调用审计。
     *
     * @param sessionId 当前会话，可为空
     * @param toolName 注册工具名称
     * @param redactedArguments 脱敏参数摘要
     * @param resultSummary 脱敏结果摘要
     * @param durationMs 调用耗时
     * @param errorCode 失败码，可为空
     */
    void record(String sessionId, String toolName, String redactedArguments, String resultSummary,
                int durationMs, String errorCode);

    /**
     * 分页读取管理员可见的脱敏审计摘要。
     *
     * @param queryDTO 请求、会话和分页条件
     * @return 审计分页结果
     */
    AgentTracePageVO findForAdmin(AgentTraceQueryDTO queryDTO);
}
