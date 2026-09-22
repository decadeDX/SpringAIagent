package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.AgentTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent 工具审计记录的数据访问入口。
 */
@Mapper
public interface AgentTraceMapper extends BaseMapper<AgentTrace> {
}
