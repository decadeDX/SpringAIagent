package io.github.decadedx.springaiagent.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在知识发布状态已成功持久化后递增缓存版本，使历史 RAG 答案立即失效。
 */
@Component
public class KnowledgePublishedVersionCacheListener {

    /** 缓存失效失败时用于记录运维诊断信息的日志入口。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgePublishedVersionCacheListener.class);

    /** 公开知识问答缓存版本服务。 */
    private final KnowledgeAnswerCache knowledgeAnswerCache;

    /**
     * 创建已发布知识缓存失效监听器。
     *
     * @param knowledgeAnswerCache 问答缓存版本服务
     */
    public KnowledgePublishedVersionCacheListener(KnowledgeAnswerCache knowledgeAnswerCache) {
        this.knowledgeAnswerCache = knowledgeAnswerCache;
    }

    /**
     * 仅在发布或停用事务成功提交后失效旧版本问答缓存。
     *
     * @param event 已提交的发布集合变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(KnowledgePublishedVersionChangedEvent event) {
        try {
            knowledgeAnswerCache.bumpPublishedVersion();
        } catch (RuntimeException exception) {
            LOGGER.warn("知识文档版本 {} 已提交，但 RAG 缓存版本递增失败", event.documentId(), exception);
        }
    }
}
