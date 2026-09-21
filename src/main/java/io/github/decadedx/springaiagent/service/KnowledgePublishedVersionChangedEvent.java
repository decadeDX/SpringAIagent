package io.github.decadedx.springaiagent.service;

/**
 * 知识文档发布集合发生变化后的事务事件；只有 MySQL 提交成功后才允许使旧问答缓存失效。
 *
 * @param documentId 已发布或停用的文档版本主键
 */
public record KnowledgePublishedVersionChangedEvent(Long documentId) {
}
