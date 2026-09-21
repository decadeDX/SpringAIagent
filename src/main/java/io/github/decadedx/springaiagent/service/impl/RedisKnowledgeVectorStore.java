package io.github.decadedx.springaiagent.service.impl;

import io.github.decadedx.springaiagent.service.KnowledgeVectorDocument;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通过 Spring AI Redis Vector Store 访问 Redis Stack，并固定知识文档版本元数据过滤语义。
 */
public class RedisKnowledgeVectorStore implements KnowledgeVectorStore {

    /** Spring AI 提供的 Redis Stack 向量访问入口。 */
    private final VectorStore vectorStore;

    /**
     * 创建 Redis Stack 向量适配器。
     *
     * @param vectorStore Spring AI 向量存储
     */
    public RedisKnowledgeVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(List<KnowledgeVectorDocument> documents) {
        vectorStore.add(documents.stream()
                .map(document -> new Document(document.vectorRecordId(), document.content(), document.metadata()))
                .toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(Collection<String> vectorRecordIds) {
        if (!vectorRecordIds.isEmpty()) {
            vectorStore.delete(List.copyOf(vectorRecordIds));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<KnowledgeVectorDocument> search(String question, Collection<Long> documentIds, int topK,
                                                double similarityThreshold) {
        if (documentIds.isEmpty()) {
            return List.of();
        }
        String values = documentIds.stream().map(String::valueOf)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(","));
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression("documentId in [" + values + "]")
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(document -> new KnowledgeVectorDocument(document.getId(), document.getText(),
                        Map.copyOf(document.getMetadata())))
                .toList();
    }
}
