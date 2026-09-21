package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentQueryDTO;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.service.KnowledgeService;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentPageVO;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供管理员上传、查询和发布知识文档版本的接口；索引由领域服务在事务提交后异步处理。
 */
@RestController
@RequestMapping("/api/admin/knowledge/documents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeDocumentController {

    /** 知识文档领域服务。 */
    private final KnowledgeService knowledgeService;

    /**
     * 创建管理员知识文档控制器。
     *
     * @param knowledgeService 知识文档领域服务
     */
    public AdminKnowledgeDocumentController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 接收管理员提交的源文件和版本元数据，返回待异步索引的文档版本。
     *
     * @param uploadDTO multipart 文档和元数据
     * @return HTTP 202 与待处理文档版本
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Result<KnowledgeDocumentVO>> upload(@Valid @ModelAttribute KnowledgeDocumentUploadDTO uploadDTO) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(knowledgeService.upload(uploadDTO)));
    }

    /**
     * 按逻辑文档编号、发布状态和索引状态分页查询所有文档版本。
     *
     * @param queryDTO 管理端筛选与分页条件
     * @return 文档版本分页结果
     */
    @GetMapping
    public Result<KnowledgeDocumentPageVO> findPage(@Valid @ModelAttribute KnowledgeDocumentQueryDTO queryDTO) {
        return Result.success(knowledgeService.findPage(queryDTO));
    }

    /**
     * 发布一个已成功建立向量索引的文档版本。
     *
     * @param documentVersionId 文档版本主键
     * @return 发布后的文档版本
     */
    @PostMapping("/{documentVersionId}/publish")
    public Result<KnowledgeDocumentVO> publish(@PathVariable Long documentVersionId) {
        return Result.success(knowledgeService.publish(documentVersionId));
    }

    /**
     * 停用一个当前已发布的文档版本，使其不再进入后续检索。
     *
     * @param documentVersionId 文档版本主键
     * @return 停用后的文档版本
     */
    @PostMapping("/{documentVersionId}/disable")
    public Result<KnowledgeDocumentVO> disable(@PathVariable Long documentVersionId) {
        return Result.success(knowledgeService.disable(documentVersionId));
    }
}
