package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.LabAvailabilityQueryDTO;
import io.github.decadedx.springaiagent.dto.LabQueryDTO;
import io.github.decadedx.springaiagent.dto.LabUpdateDTO;
import io.github.decadedx.springaiagent.vo.LabAvailabilityVO;
import io.github.decadedx.springaiagent.vo.LabPageVO;
import io.github.decadedx.springaiagent.vo.LabVO;

/**
 * 提供实验室资料筛选与实时可用性查询；查询结果不替代预约确认时的最终校验。
 */
public interface LabService {

    /**
     * 按名称、设备关键词和最低容量筛选实验室。
     *
     * @param queryDTO 分页与筛选条件
     * @return 实验室分页摘要
     */
    LabPageVO search(LabQueryDTO queryDTO);

    /**
     * 查询一个实验室的基础详情，允许 Redis 缓存故障时回源 MySQL。
     *
     * @param labId 实验室业务编号
     * @return 当前实验室详情
     */
    LabVO findDetail(String labId);

    /**
     * 管理员更新实验室白名单字段，并在事务提交后失效详情缓存。
     *
     * @param labId 实验室业务编号
     * @param updateDTO 待更新字段
     * @return 更新后的详情
     */
    LabVO update(String labId, LabUpdateDTO updateDTO);

    /**
     * 查询指定上海日期内的整点时隙是否可用。
     *
     * @param labId 实验室业务编号
     * @param queryDTO 日期与可选时间范围
     * @return 开放时段及可用时隙
     */
    LabAvailabilityVO availability(String labId, LabAvailabilityQueryDTO queryDTO);
}
