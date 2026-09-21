package io.github.decadedx.springaiagent.service;

import java.util.List;

/**
 * 模型在受控 RAG Prompt 下返回的内部 JSON 结构，不直接暴露给 HTTP 客户端。
 *
 * @param answer 基于资料生成的回答文本
 * @param citations 候选分块引用
 */
public record RagModelResponse(String answer, List<RagModelCitation> citations) {
}
