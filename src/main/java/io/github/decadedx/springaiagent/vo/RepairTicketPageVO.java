package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 当前学生本人报修工单的分页结果。
 *
 * @param items 本页工单
 * @param page 当前页码
 * @param size 每页数量
 * @param total 满足筛选条件的总数
 */
public record RepairTicketPageVO(List<RepairTicketVO> items, int page, int size, long total) {
}
