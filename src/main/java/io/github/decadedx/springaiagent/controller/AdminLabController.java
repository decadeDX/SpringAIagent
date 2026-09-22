package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.LabUpdateDTO;
import io.github.decadedx.springaiagent.service.LabService;
import io.github.decadedx.springaiagent.vo.LabVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员维护实验室基础资料；缓存失效由领域服务在事务提交后执行。
 */
@RestController
@RequestMapping("/api/admin/labs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLabController {

    /** 实验室服务。 */
    private final LabService labService;

    /**
     * 创建管理员实验室控制器。
     *
     * @param labService 实验室服务
     */
    public AdminLabController(LabService labService) {
        this.labService = labService;
    }

    /**
     * 更新实验室可维护字段。
     *
     * @param labId 实验室业务编号
     * @param updateDTO 白名单更新字段
     * @return 更新后的详情
     */
    @PatchMapping("/{labId}")
    public Result<LabVO> update(@PathVariable String labId, @Valid @RequestBody LabUpdateDTO updateDTO) {
        return Result.success(labService.update(labId, updateDTO));
    }
}
