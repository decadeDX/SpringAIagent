package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.ActionConfirmDTO;
import io.github.decadedx.springaiagent.vo.ActionExecutionVO;

/**
 * 将用户点击确认转换为一次可幂等重放的领域写入。
 */
public interface ActionConfirmationService {

    /**
     * 执行或重放指定草案动作。
     *
     * @param actionId 服务端生成的草案标识
     * @param confirmDTO 客户端回传的会话绑定
     * @return 成功结果或成功重放结果
     */
    ActionExecutionVO confirm(String actionId, ActionConfirmDTO confirmDTO);
}
