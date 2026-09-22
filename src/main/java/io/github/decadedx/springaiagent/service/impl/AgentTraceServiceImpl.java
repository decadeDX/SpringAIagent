package io.github.decadedx.springaiagent.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.decadedx.springaiagent.common.RequestIdContext;
import io.github.decadedx.springaiagent.config.TimeConfig;
import io.github.decadedx.springaiagent.dto.AgentTraceQueryDTO;
import io.github.decadedx.springaiagent.entity.AgentTrace;
import io.github.decadedx.springaiagent.mapper.AgentTraceMapper;
import io.github.decadedx.springaiagent.security.CurrentUser;
import io.github.decadedx.springaiagent.service.AgentTraceService;
import io.github.decadedx.springaiagent.vo.AgentTracePageVO;
import io.github.decadedx.springaiagent.vo.AgentTraceVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneOffset;

/**
 * 将 Agent 工具调用摘要持久化到 MySQL，供管理员按请求或会话审计。
 */
@Service
public class AgentTraceServiceImpl implements AgentTraceService {

    /** 审计数据访问入口。 */
    private final AgentTraceMapper agentTraceMapper;

    /**
     * 创建审计服务。
     *
     * @param agentTraceMapper 审计 Mapper
     */
    public AgentTraceServiceImpl(AgentTraceMapper agentTraceMapper) {
        this.agentTraceMapper = agentTraceMapper;
    }

    /** {@inheritDoc} */
    @Override
    public void record(String sessionId, String toolName, String redactedArguments, String resultSummary,
                       int durationMs, String errorCode) {
        AgentTrace trace = new AgentTrace();
        trace.setId(IdWorker.getId());
        trace.setRequestId(RequestIdContext.currentOrCreate());
        trace.setSessionId(sessionId);
        trace.setUserId(CurrentUser.requireId());
        trace.setToolName(toolName);
        trace.setRedactedArguments(redactedArguments);
        trace.setResultSummary(resultSummary);
        trace.setDurationMs(Math.max(0, durationMs));
        trace.setErrorCode(errorCode);
        agentTraceMapper.insert(trace);
    }

    /** {@inheritDoc} */
    @Override
    public AgentTracePageVO findForAdmin(AgentTraceQueryDTO queryDTO) {
        int page = queryDTO.page() == null ? 1 : queryDTO.page();
        int size = queryDTO.size() == null ? 20 : queryDTO.size();
        LambdaQueryWrapper<AgentTrace> query = new LambdaQueryWrapper<AgentTrace>()
                .eq(StringUtils.hasText(queryDTO.requestId()), AgentTrace::getRequestId, queryDTO.requestId())
                .eq(StringUtils.hasText(queryDTO.sessionId()), AgentTrace::getSessionId, queryDTO.sessionId())
                .orderByDesc(AgentTrace::getCreatedAt);
        Page<AgentTrace> result = agentTraceMapper.selectPage(new Page<>(page, size), query);
        return new AgentTracePageVO(result.getRecords().stream().map(this::toVO).toList(), page, size,
                result.getTotal());
    }

    /**
     * 转换为不暴露参数 JSON 或用户内部主键的管理端摘要。
     *
     * @param trace 持久化审计记录
     * @return 脱敏审计视图
     */
    private AgentTraceVO toVO(AgentTrace trace) {
        return new AgentTraceVO(trace.getId(), trace.getRequestId(), trace.getSessionId(), trace.getToolName(),
                trace.getResultSummary(), trace.getDurationMs(), trace.getErrorCode(), trace.getCreatedAt() == null ? null
                : trace.getCreatedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(TimeConfig.BUSINESS_ZONE).toOffsetDateTime());
    }
}
