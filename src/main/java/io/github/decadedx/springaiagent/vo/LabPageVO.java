package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 实验室列表的分页响应，页码从 1 开始。
 *
 * @param items 当前页实验室摘要
 * @param page 当前页码
 * @param size 每页数量
 * @param total 满足筛选条件的总数
 */
public record LabPageVO(List<LabVO> items, int page, int size, long total) {
}
