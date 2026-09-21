package io.github.decadedx.springaiagent.service;

import java.util.Map;

/**
 * 向量库读写使用的最小文档载荷，避免领域服务依赖具体 Redis 客户端 API。
 *
 * @param vectorRecordId 向量库中稳定且可删除的记录标识
 * @param content 分块原文
 * @param metadata 用于版本过滤与引用关联的扁平元数据
 */
public record KnowledgeVectorDocument(String vectorRecordId, String content, Map<String, Object> metadata) {
}
