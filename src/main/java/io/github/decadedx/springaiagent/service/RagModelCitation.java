package io.github.decadedx.springaiagent.service;

/**
 * 模型结构化输出中的单条候选引用；必须经 CitationValidator 验证后才能返回用户。
 *
 * @param chunkId 模型声明的分块标识
 * @param excerpt 模型声明的原文证据片段
 */
public record RagModelCitation(String chunkId, String excerpt) {
}
