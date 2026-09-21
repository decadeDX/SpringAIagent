package io.github.decadedx.springaiagent.service;

/**
 * 定义知识文档后台索引和应用启动恢复的服务边界。
 */
public interface KnowledgeIndexService {

    /**
     * 异步尝试领取并索引一个待处理文档版本。
     *
     * @param documentId 文档版本主键
     */
    void schedule(Long documentId);

    /**
     * 将启动前中断的索引任务恢复为待处理状态，并重新安排所有待处理版本。
     */
    void recoverPending();
}
