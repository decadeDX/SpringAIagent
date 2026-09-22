package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.LabAvailabilityQueryDTO;
import io.github.decadedx.springaiagent.dto.LabQueryDTO;
import io.github.decadedx.springaiagent.service.LabService;
import io.github.decadedx.springaiagent.vo.LabAvailabilityVO;
import io.github.decadedx.springaiagent.vo.LabPageVO;
import io.github.decadedx.springaiagent.vo.LabVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供已登录用户可调用的实验室筛选与时隙查询接口。
 */
@RestController
@RequestMapping("/api/labs")
@PreAuthorize("isAuthenticated()")
public class LabController {

    /** 实验室查询业务服务。 */
    private final LabService labService;

    /**
     * 创建实验室控制器。
     *
     * @param labService 实验室查询服务
     */
    public LabController(LabService labService) {
        this.labService = labService;
    }

    /**
     * 分页筛选当前可见的实验室基础资料。
     *
     * @param queryDTO 名称、设备、容量和分页条件
     * @return 实验室分页摘要
     */
    @GetMapping
    public Result<LabPageVO> search(@Valid @ModelAttribute LabQueryDTO queryDTO) {
        return Result.success(labService.search(queryDTO));
    }

    /**
     * 查询指定实验室的基础详情；缓存故障时仍由服务回源数据库。
     *
     * @param labId 实验室业务编号
     * @return 实验室详情
     */
    @GetMapping("/{labId}")
    public Result<LabVO> findDetail(@PathVariable String labId) {
        return Result.success(labService.findDetail(labId));
    }

    /**
     * 查询指定日期和时间范围内的整点可用性。
     *
     * @param labId 实验室业务编号
     * @param queryDTO 日期与可选时间范围
     * @return 实验室开放时段和可用时隙
     */
    @GetMapping("/{labId}/availability")
    public Result<LabAvailabilityVO> availability(@PathVariable String labId,
                                                  @Valid @ModelAttribute LabAvailabilityQueryDTO queryDTO) {
        return Result.success(labService.availability(labId, queryDTO));
    }
}
