package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.ReservationCancelPayload;
import io.github.decadedx.springaiagent.dto.ReservationCreatePayload;
import io.github.decadedx.springaiagent.dto.ReservationDraftCreateDTO;
import io.github.decadedx.springaiagent.dto.ReservationQueryDTO;
import io.github.decadedx.springaiagent.vo.ReservationPageVO;
import io.github.decadedx.springaiagent.vo.ReservationVO;

/**
 * 处理当前用户预约的准备、确认和查询；模块 05 负责把准备结果封装为可确认草案。
 */
public interface ReservationService {

    /**
     * 校验创建预约所需规则并生成可供草案服务保存的标准化载荷，不写入业务表。
     *
     * @param createDTO 未绑定用户标识的预约输入
     * @return 已校验的预约载荷
     */
    ReservationCreatePayload prepareCreate(ReservationDraftCreateDTO createDTO);

    /**
     * 在事务中重新校验并创建预约及全部整点时隙。
     *
     * @param payload 已从可信草案恢复的预约载荷
     * @return 已确认预约
     */
    ReservationVO confirmCreate(ReservationCreatePayload payload);

    /**
     * 校验当前用户是否可以取消指定预约，不修改业务事实。
     *
     * @param reservationId 待取消预约主键
     * @return 可供草案服务保存的取消载荷
     */
    ReservationCancelPayload prepareCancel(Long reservationId);

    /**
     * 在事务中重新校验取消边界并释放预约时隙。
     *
     * @param payload 已从可信草案恢复的取消载荷
     * @return 已取消预约
     */
    ReservationVO confirmCancel(ReservationCancelPayload payload);

    /**
     * 分页查询当前认证学生自己的预约，永远不会返回其他用户数据。
     *
     * @param queryDTO 状态、时间窗口和分页条件
     * @return 当前用户预约分页结果
     */
    ReservationPageVO findMine(ReservationQueryDTO queryDTO);
}
