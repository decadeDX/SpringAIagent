package io.github.decadedx.springaiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.decadedx.springaiagent.entity.RepairTicket;
import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报修工单聚合根的持久化入口；状态流转通过版本号比较更新。
 */
@Mapper
public interface RepairTicketMapper extends BaseMapper<RepairTicket> {

    /**
     * 分页查询指定学生的工单，身份过滤由调用方固定传入。
     *
     * @param userId 当前学生主键
     * @param status 可选状态条件
     * @param offset 从零开始的偏移量
     * @param size 本页数量
     * @return 当前学生可见的工单
     */
    List<RepairTicket> selectMine(@Param("userId") Long userId, @Param("status") RepairTicketStatus status,
                                  @Param("offset") long offset, @Param("size") int size);

    /**
     * 统计指定学生符合条件的工单数量。
     *
     * @param userId 当前学生主键
     * @param status 可选状态条件
     * @return 工单总数
     */
    long countMine(@Param("userId") Long userId, @Param("status") RepairTicketStatus status);

    /**
     * 锁定工单行以读取当前状态和版本，防止并发处理产生非法跳转。
     *
     * @param ticketId 工单主键
     * @return 被锁定的工单；不存在时为空
     */
    RepairTicket selectByIdForUpdate(@Param("ticketId") Long ticketId);

    /**
     * 在状态和版本仍保持预期值时完成管理员处理。
     *
     * @param ticketId 工单主键
     * @param status 目标状态
     * @param resolutionNote 本次处理说明
     * @param processedBy 当前管理员主键
     * @param version 读取到的乐观锁版本
     * @return 受影响行数，零表示记录已被并发修改
     */
    int updateProcess(@Param("ticketId") Long ticketId, @Param("status") RepairTicketStatus status,
                      @Param("resolutionNote") String resolutionNote, @Param("processedBy") Long processedBy,
                      @Param("version") Long version);
}
