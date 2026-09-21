package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 当前用户预约列表的分页响应，页码从 1 开始。
 *
 * @param items 当前页预约
 * @param page 当前页码
 * @param size 每页数量
 * @param total 满足筛选条件的总数
 */
public record ReservationPageVO(List<ReservationVO> items, int page, int size, long total) {
}
