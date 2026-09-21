package io.github.decadedx.springaiagent.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 在上传事务提交后才触发异步索引，避免后台线程读取未提交文档记录。
 */
@Component
@ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
public class KnowledgeIndexRequestListener {

    /** 后台索引服务。 */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 创建上传后索引事件监听器。
     *
     * @param knowledgeIndexService 后台索引服务
     */
    public KnowledgeIndexRequestListener(KnowledgeIndexService knowledgeIndexService) {
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 在所属数据库事务成功提交后安排索引任务。
     *
     * @param event 已提交文档版本事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void schedule(KnowledgeIndexRequestedEvent event) {
        knowledgeIndexService.schedule(event.documentId());
    }
}
