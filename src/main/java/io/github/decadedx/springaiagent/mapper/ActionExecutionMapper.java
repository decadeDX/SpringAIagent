package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.ActionExecution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 动作执行幂等记录的数据访问入口。
 */
@Mapper
public interface ActionExecutionMapper extends BaseMapper<ActionExecution> {

    /**
     * 锁定指定动作的执行记录，串行化重复确认。
     *
     * @param actionId 动作标识
     * @return 已存在记录或空
     */
    ActionExecution selectByActionIdForUpdate(@Param("actionId") String actionId);
}
