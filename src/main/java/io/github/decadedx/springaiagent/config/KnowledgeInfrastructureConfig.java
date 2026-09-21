package io.github.decadedx.springaiagent.config;

import io.github.decadedx.springaiagent.service.RagChatClient;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;
import io.github.decadedx.springaiagent.service.impl.RedisKnowledgeVectorStore;
import io.github.decadedx.springaiagent.service.impl.SpringAiRagChatClient;
import io.github.decadedx.springaiagent.service.impl.UnavailableKnowledgeVectorStore;
import io.github.decadedx.springaiagent.service.impl.UnavailableRagChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 仅在显式启用 RAG 后装配真实 OpenAI 与 Redis Stack 适配器，避免未配置密钥的环境误用外部依赖。
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeInfrastructureConfig {

    /**
     * 创建受限的知识文档索引线程池，避免大文件索引占满 Web 请求线程。
     *
     * @return 异步索引任务执行器
     */
    @Bean("knowledgeIndexExecutor")
    public Executor knowledgeIndexExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("knowledge-index-");
        executor.initialize();
        return executor;
    }

    /**
     * 创建 Redis Stack 向量适配器。
     *
     * @param vectorStore Spring AI 配置的 Redis 向量存储
     * @return 真实向量存储实现
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public KnowledgeVectorStore redisKnowledgeVectorStore(VectorStore vectorStore) {
        return new RedisKnowledgeVectorStore(vectorStore);
    }

    /**
     * 创建仅发送无工具 RAG Prompt 的 OpenAI 聊天适配器。
     *
     * @param chatModel Spring AI 自动配置的聊天模型
     * @return 真实聊天适配器
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public RagChatClient springAiRagChatClient(ChatModel chatModel) {
        return new SpringAiRagChatClient(chatModel);
    }

    /**
     * 在 RAG 未启用时提供明确失败的向量访问入口，而不回退到内存检索。
     *
     * @return 不可用向量存储实现
     */
    @Bean
    @ConditionalOnMissingBean(KnowledgeVectorStore.class)
    public KnowledgeVectorStore unavailableKnowledgeVectorStore() {
        return new UnavailableKnowledgeVectorStore();
    }

    /**
     * 在 RAG 未启用时提供明确失败的聊天访问入口，避免生成无依据回答。
     *
     * @return 不可用聊天实现
     */
    @Bean
    @ConditionalOnMissingBean(RagChatClient.class)
    public RagChatClient unavailableRagChatClient() {
        return new UnavailableRagChatClient();
    }
}
