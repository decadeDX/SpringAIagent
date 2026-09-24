package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.ActionExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动作执行幂等记录的数据访问入口。
 */
@Mapper
public interface ActionExecutionMapper extends BaseMapper<ActionExecution> {
}
