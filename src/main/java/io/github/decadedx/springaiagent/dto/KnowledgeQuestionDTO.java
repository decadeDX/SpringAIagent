package io.github.decadedx.springaiagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 已登录用户向独立知识库问答接口提交的问题；不携带用户实时业务数据。
 *
 * @param question 待检索的问题文本
 */
public record KnowledgeQuestionDTO(
        @NotBlank(message = "问题不能为空") @Size(max = 2000, message = "问题不能超过2000个字符") String question
) {
}
