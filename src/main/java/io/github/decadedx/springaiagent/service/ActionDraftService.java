package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.enums.ActionType;
import io.github.decadedx.springaiagent.vo.ActionDraftVO;

import java.util.List;

/**
 * 管理绑定用户和会话的短期确认草案；Redis 中的草案永不作为最终业务事实。
 */
public interface ActionDraftService {

    /**
     * 为当前认证用户创建草案；未指定会话时创建独立业务会话。
     *
     * @param actionType 草案动作类型
     * @param sessionId 聊天会话标识，可为空
     * @param payload 已通过领域准备校验的可信载荷
     * @param notices 面向用户的限制或安全提示
     * @return 可返回给前端核对的草案
     */
    ActionDraftVO create(ActionType actionType, String sessionId, Object payload, List<String> notices);

    /**
     * 读取确认阶段使用的草案，并验证 Redis 可用性。
     *
     * @param actionId 草案动作标识
     * @return 已恢复的内部草案；不存在时为空
     */
    StoredActionDraft find(String actionId);

    /**
     * 在确认成功后删除短期草案。
     *
     * @param actionId 已完成动作标识
     */
    void delete(String actionId);
}
