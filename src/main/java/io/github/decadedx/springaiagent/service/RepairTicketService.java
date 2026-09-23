package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.RepairDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketCreatePayload;
import io.github.decadedx.springaiagent.dto.RepairTicketQueryDTO;
import io.github.decadedx.springaiagent.dto.RepairTicketUpdateDTO;
import io.github.decadedx.springaiagent.vo.RepairTicketPageVO;
import io.github.decadedx.springaiagent.vo.RepairTicketVO;

/**
 * 定义报修草案校验、确认创建、本人查询和管理员工单流转的领域边界。
 */
public interface RepairTicketService {

    /**
     * 校验当前学生提交的报修输入，并生成可由模块 05 缓存的确认载荷。
     *
     * @param createDTO 原始报修草案输入
     * @return 已校验的创建载荷及必要安全提示
     */
    RepairTicketCreatePayload prepareCreate(RepairDraftCreateDTO createDTO);

    /**
     * 在事务内重新校验草案载荷并持久化一张已提交工单。
     *
     * @param payload 模块 05 恢复的已校验载荷
     * @return 新创建的工单
     */
    RepairTicketVO confirmCreate(RepairTicketCreatePayload payload);

    /**
     * 分页查询当前学生自己的工单，查询条件不包含外部用户身份。
     *
     * @param queryDTO 状态和分页条件
     * @return 当前学生的工单分页结果
     */
    RepairTicketPageVO findMine(RepairTicketQueryDTO queryDTO);

    /**
     * 分页查询全部工单，供管理员处理待办和追踪状态。
     *
     * @param queryDTO 状态和分页条件
     * @return 管理员可见的工单分页结果
     */
    RepairTicketPageVO findAll(RepairTicketQueryDTO queryDTO);

    /**
     * 管理员按相邻状态处理工单，并记录本次处理说明和处理人。
     *
     * @param ticketId 待处理工单主键
     * @param updateDTO 目标状态和处理说明
     * @return 更新后的工单
     */
    RepairTicketVO process(Long ticketId, RepairTicketUpdateDTO updateDTO);
}
