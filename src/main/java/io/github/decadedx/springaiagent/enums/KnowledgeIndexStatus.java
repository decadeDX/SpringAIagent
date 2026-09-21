package io.github.decadedx.springaiagent.enums;

/**
 * 知识文档版本向量化处理状态；发布前必须达到 SUCCEEDED。
 */
public enum KnowledgeIndexStatus {
    /** 已保存源文件，等待后台任务处理。 */
    PENDING,
    /** 正在切分、嵌入并写入向量库。 */
    INDEXING,
    /** 分块与向量写入均已完成。 */
    SUCCEEDED,
    /** 索引失败，失败原因保存在文档版本中。 */
    FAILED
}
