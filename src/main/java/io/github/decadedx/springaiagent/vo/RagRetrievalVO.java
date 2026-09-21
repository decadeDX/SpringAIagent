package io.github.decadedx.springaiagent.vo;

/**
 * 本轮 RAG 检索的不可敏感性能摘要。
 *
 * @param topK 请求的最大返回分块数
 * @param hitCount 实际命中的有效分块数
 * @param retrievalMs 从向量检索开始到生成结果前的耗时毫秒数
 */
public record RagRetrievalVO(int topK, int hitCount, long retrievalMs) {
}
