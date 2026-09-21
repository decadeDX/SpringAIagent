package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.KnowledgeQuestionDTO;
import io.github.decadedx.springaiagent.service.RagService;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供已登录用户的独立知识库问答接口；该入口不查询或注入任何个人实时业务数据。
 */
@RestController
@RequestMapping("/api/knowledge/questions")
@PreAuthorize("isAuthenticated()")
public class KnowledgeQuestionController {

    /** 独立 RAG 问答服务。 */
    private final RagService ragService;

    /**
     * 创建知识库问答控制器。
     *
     * @param ragService 独立 RAG 问答服务
     */
    public KnowledgeQuestionController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 根据当前已发布且索引成功的资料回答问题，并返回经过校验的引用。
     *
     * @param questionDTO 用户问题
     * @return 问答结果或固定无依据回答
     */
    @PostMapping
    public Result<RagAnswerVO> ask(@Valid @RequestBody KnowledgeQuestionDTO questionDTO) {
        return Result.success(ragService.ask(questionDTO));
    }
}
