package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.Lab;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实验室基础资料的单表访问入口。
 */
@Mapper
public interface LabMapper extends BaseMapper<Lab> {
}
