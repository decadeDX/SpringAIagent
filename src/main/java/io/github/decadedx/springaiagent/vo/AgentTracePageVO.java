package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 管理员 Agent 审计记录分页结果。
 */
public record AgentTracePageVO(List<AgentTraceVO> items, int page, int size, long total) {
}
