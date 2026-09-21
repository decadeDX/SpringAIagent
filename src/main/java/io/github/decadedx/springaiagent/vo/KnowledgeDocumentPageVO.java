package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 管理员知识文档版本列表的分页响应。
 *
 * @param items 当前页文档版本
 * @param page 当前页码
 * @param size 每页数量
 * @param total 满足条件的文档版本总数
 */
public record KnowledgeDocumentPageVO(List<KnowledgeDocumentVO> items, int page, int size, long total) {
}
