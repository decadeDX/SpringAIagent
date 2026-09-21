package io.github.decadedx.springaiagent.dto;

import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 管理员分页查询知识文档版本时使用的筛选条件。
 *
 * @param logicalDocumentCode 可选逻辑文档编号
 * @param publishStatus 可选发布状态
 * @param indexStatus 可选索引状态
 * @param page 从 1 开始的页码
 * @param size 每页数量
 */
public record KnowledgeDocumentQueryDTO(
        @Size(max = 64, message = "逻辑文档编号不能超过64个字符") String logicalDocumentCode,
        KnowledgePublishStatus publishStatus,
        KnowledgeIndexStatus indexStatus,
        @Min(value = 1, message = "页码至少为1") Integer page,
        @Min(value = 1, message = "每页数量至少为1")
        @Max(value = 50, message = "每页数量不能超过50") Integer size
) {
}
