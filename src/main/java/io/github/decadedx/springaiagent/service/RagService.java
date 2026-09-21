package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.KnowledgeQuestionDTO;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;

/**
 * 定义只依赖已发布知识文档的独立检索增强问答服务。
 */
public interface RagService {

    /**
     * 根据当前已发布且索引成功的知识分块回答问题，并返回可验证引用。
     *
     * @param questionDTO 已校验的问题输入
     * @return 带引用或固定拒答语的问答结果
     */
    RagAnswerVO ask(KnowledgeQuestionDTO questionDTO);
}
