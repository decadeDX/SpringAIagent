package io.github.decadedx.springaiagent.vo;

import java.util.List;

/**
 * 独立知识库问答结果；回答只能由 citations 指向的本轮检索资料支持。
 *
 * @param answer 回答文本或固定无依据拒答语
 * @param citations 已验证引用集合
 * @param retrieval 本轮检索摘要
 */
public record RagAnswerVO(String answer, List<KnowledgeCitationVO> citations, RagRetrievalVO retrieval) {
}
