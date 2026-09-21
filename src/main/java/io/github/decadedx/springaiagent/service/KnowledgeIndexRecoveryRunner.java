package io.github.decadedx.springaiagent.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 真实 RAG 启用时恢复应用异常退出前未完成的知识索引任务。
 */
@Component
@ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
public class KnowledgeIndexRecoveryRunner implements ApplicationRunner {

    /** 后台索引服务。 */
    private final KnowledgeIndexService knowledgeIndexService;

    /**
     * 创建索引恢复启动器。
     *
     * @param knowledgeIndexService 后台索引服务
     */
    public KnowledgeIndexRecoveryRunner(KnowledgeIndexService knowledgeIndexService) {
        this.knowledgeIndexService = knowledgeIndexService;
    }

    /**
     * 应用就绪后恢复并重新调度遗留索引任务。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        knowledgeIndexService.recoverPending();
    }
}
