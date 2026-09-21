package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.LabAvailabilityQueryDTO;
import io.github.decadedx.springaiagent.dto.LabQueryDTO;
import io.github.decadedx.springaiagent.vo.LabAvailabilityVO;
import io.github.decadedx.springaiagent.vo.LabPageVO;

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
     * 查询指定上海日期内的整点时隙是否可用。
     *
     * @param labId 实验室业务编号
     * @param queryDTO 日期与可选时间范围
     * @return 开放时段及可用时隙
     */
    LabAvailabilityVO availability(String labId, LabAvailabilityQueryDTO queryDTO);
}
