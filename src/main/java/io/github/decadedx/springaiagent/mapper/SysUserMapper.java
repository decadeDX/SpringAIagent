package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户的单表持久化入口；登录查询将在认证服务中通过该 Mapper 执行。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 锁定当前用户行，以串行化有效预约上限检查并读取最新培训状态。
     *
     * @param userId 当前认证用户主键
     * @return 被锁定的用户；账号已删除时为空
     */
    SysUser selectByIdForUpdate(@Param("userId") Long userId);
}
