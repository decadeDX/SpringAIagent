package io.github.decadedx.springaiagent.service;

/**
 * 表示知识文档上传事务已提交，可安全交由异步线程开始索引的领域事件。
 *
 * @param documentId 已提交文档版本主键
 */
public record KnowledgeIndexRequestedEvent(Long documentId) {
}
