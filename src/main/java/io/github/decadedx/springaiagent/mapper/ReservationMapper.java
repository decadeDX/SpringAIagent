package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.Reservation;
import io.github.decadedx.springaiagent.enums.ReservationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约聚合根的数据访问入口；涉及归属、计数和锁定的 SQL 定义在 XML 中。
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 分页查询当前用户在给定窗口内相交的预约，并填充实验室名称。
     *
     * @param userId 当前认证用户主键
     * @param status 可选状态筛选
     * @param from 可选 UTC 窗口开始
     * @param to 可选 UTC 窗口结束
     * @param offset 分页偏移量
     * @param size 每页数量
     * @return 当前页预约列表
     */
    List<Reservation> selectMine(@Param("userId") Long userId,
                                 @Param("status") ReservationStatus status,
                                 @Param("from") LocalDateTime from,
                                 @Param("to") LocalDateTime to,
                                 @Param("offset") long offset,
                                 @Param("size") int size);

    /**
     * 统计当前用户在给定窗口内相交的预约数量。
     *
     * @param userId 当前认证用户主键
     * @param status 可选状态筛选
     * @param from 可选 UTC 窗口开始
     * @param to 可选 UTC 窗口结束
     * @return 匹配记录数量
     */
    long countMine(@Param("userId") Long userId,
                   @Param("status") ReservationStatus status,
                   @Param("from") LocalDateTime from,
                   @Param("to") LocalDateTime to);

    /**
     * 锁定预约行，供取消确认重新检查归属和时间边界。
     *
     * @param reservationId 预约主键
     * @return 被锁定的预约；不存在时为空
     */
    Reservation selectByIdForUpdate(@Param("reservationId") Long reservationId);

    /**
     * 在用户行锁保护下统计尚未结束的有效预约。
     *
     * @param userId 当前认证用户主键
     * @param now 当前 UTC 时刻
     * @return 仍占用用户预约额度的有效预约数量
     */
    long countActiveByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /**
     * 将仍有效的预约取消并记录取消时刻。
     *
     * @param reservationId 已锁定的预约主键
     * @param cancelledAt UTC 取消时刻
     * @return 成功更新的记录数
     */
    int cancelConfirmed(@Param("reservationId") Long reservationId,
                        @Param("cancelledAt") LocalDateTime cancelledAt);
}
