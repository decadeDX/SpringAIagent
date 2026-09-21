package io.github.decadedx.springaiagent.vo;

/**
 * 一条经过验证、且属于本轮检索集合的 RAG 证据引用。
 *
 * @param documentTitle 文档标题
 * @param version 文档版本文本
 * @param chunkId 本轮命中的稳定分块标识
 * @param excerpt 在分块原文中可验证的证据片段
 */
public record KnowledgeCitationVO(String documentTitle, String version, String chunkId, String excerpt) {
}
