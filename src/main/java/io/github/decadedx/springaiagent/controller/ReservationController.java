package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.ReservationQueryDTO;
import io.github.decadedx.springaiagent.service.ReservationService;
import io.github.decadedx.springaiagent.vo.ReservationPageVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供学生本人预约的查询接口；草案与确认端点由模块 05 统一提供。
 */
@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasRole('STUDENT')")
public class ReservationController {

    /** 当前用户预约领域服务。 */
    private final ReservationService reservationService;

    /**
     * 创建预约控制器。
     *
     * @param reservationService 预约领域服务
     */
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * 分页查询当前学生自己的预约，服务层固定以认证身份过滤。
     *
     * @param queryDTO 状态、时间窗口和分页条件
     * @return 本人预约分页结果
     */
    @GetMapping("/me")
    public Result<ReservationPageVO> findMine(@Valid @ModelAttribute ReservationQueryDTO queryDTO) {
        return Result.success(reservationService.findMine(queryDTO));
    }
}
