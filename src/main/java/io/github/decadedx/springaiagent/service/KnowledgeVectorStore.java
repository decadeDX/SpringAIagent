package io.github.decadedx.springaiagent.service;

import java.util.Collection;
import java.util.List;

/**
 * 抽象 Redis Stack 向量读写所需的最小能力，为业务测试提供可替换边界。
 */
public interface KnowledgeVectorStore {

    /**
     * 批量写入已完成嵌入的知识分块。
     *
     * @param documents 待写入的向量文档
     */
    void add(List<KnowledgeVectorDocument> documents);

    /**
     * 按稳定记录标识物理删除向量文档。
     *
     * @param vectorRecordIds 待删除记录标识
     */
    void delete(Collection<String> vectorRecordIds);

    /**
     * 仅从指定已发布文档版本中进行相似度检索。
     *
     * @param question 用户问题
     * @param documentIds 已发布且索引成功的文档版本主键
     * @param topK 最大命中数
     * @param similarityThreshold 最低相似度阈值
     * @return 命中的向量文档
     */
    List<KnowledgeVectorDocument> search(String question, Collection<Long> documentIds, int topK,
                                         double similarityThreshold);
}
