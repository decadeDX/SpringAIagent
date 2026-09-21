package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.ReservationSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约整点占用记录的访问入口；唯一键竞争由数据库而非预查询结果决定。
 */
@Mapper
public interface ReservationSlotMapper extends BaseMapper<ReservationSlot> {

    /**
     * 查询给定 UTC 区间内已经被占用的整点时隙。
     *
     * @param labId 实验室业务编号
     * @param startTime 查询开始时刻，含
     * @param endTime 查询结束时刻，不含
     * @return 被占用时隙的 UTC 开始时刻
     */
    List<LocalDateTime> selectOccupiedSlotStarts(@Param("labId") String labId,
                                                 @Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 批量写入同一预约生成的时隙；任何重复键均应导致调用事务回滚。
     *
     * @param slots 预约对应的整点时隙
     * @return 插入记录数
     */
    int insertBatch(@Param("slots") List<ReservationSlot> slots);

    /**
     * 删除已取消预约占用的全部时隙。
     *
     * @param reservationId 已取消预约主键
     * @return 删除记录数
     */
    int deleteByReservationId(@Param("reservationId") Long reservationId);
}
