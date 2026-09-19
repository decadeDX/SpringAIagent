package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户的单表持久化入口；登录查询将在认证服务中通过该 Mapper 执行。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
