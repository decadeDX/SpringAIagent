package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.service.KnowledgeVectorDocument;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;

import java.util.Collection;
import java.util.List;

/**
 * 在未启用真实 RAG 基础设施时拒绝向量操作，禁止退回内存或字符串匹配实现。
 */
public class UnavailableKnowledgeVectorStore implements KnowledgeVectorStore {

    /** 未启用 RAG 时使用的固定失败信息。 */
    private static final String MESSAGE = "知识库向量服务未启用";

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(List<KnowledgeVectorDocument> documents) {
        throw unavailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Collection<String> vectorRecordIds) {
        throw unavailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<KnowledgeVectorDocument> search(String question, Collection<Long> documentIds, int topK,
                                                double similarityThreshold) {
        throw unavailable();
    }

    /**
     * 创建明确指向部署配置问题的失败异常。
     *
     * @return 基础设施未启用异常
     */
    private IllegalStateException unavailable() {
        return new IllegalStateException(MESSAGE);
    }
}
