package io.github.decadedx.springaiagent.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 管理员可见的知识文档版本摘要，不暴露服务器本地源文件路径。
 *
 * @param id 文档版本主键
 * @param logicalDocumentCode 逻辑文档编号
 * @param version 文档版本文本
 * @param title 文档标题
 * @param effectiveAt 上海时区的生效时刻
 * @param applicableLabIds 适用实验室编号集合
 * @param publishStatus 当前发布状态
 * @param indexStatus 当前索引状态
 * @param chunkCount 已持久化的分块数
 * @param indexFailureReason 最近索引失败原因
 */
public record KnowledgeDocumentVO(Long id, String logicalDocumentCode, String version, String title,
                                  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX") OffsetDateTime effectiveAt,
                                  List<String> applicableLabIds, KnowledgePublishStatus publishStatus,
                                  KnowledgeIndexStatus indexStatus, int chunkCount, String indexFailureReason) {
}
