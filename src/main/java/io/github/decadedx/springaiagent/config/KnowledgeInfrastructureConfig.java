package io.github.decadedx.springaiagent.config;

import io.github.decadedx.springaiagent.service.RagChatClient;
import io.github.decadedx.springaiagent.service.AgentModelClient;
import io.github.decadedx.springaiagent.service.KnowledgeVectorStore;
import io.github.decadedx.springaiagent.common.ApplicationMetrics;
import io.github.decadedx.springaiagent.service.impl.RedisKnowledgeVectorStore;
import io.github.decadedx.springaiagent.service.impl.SpringAiRagChatClient;
import io.github.decadedx.springaiagent.service.impl.UnavailableKnowledgeVectorStore;
import io.github.decadedx.springaiagent.service.impl.UnavailableRagChatClient;
import io.github.decadedx.springaiagent.service.impl.SpringAiAgentModelClient;
import io.github.decadedx.springaiagent.service.impl.UnavailableAgentModelClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

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
     * 创建支持按文档版本过滤的 Redis Stack 向量存储。
     *
     * @param embeddingModel 文本嵌入模型
     * @param connectionFactory 复用应用 Redis 连接配置的 Jedis 工厂
     * @param indexName Redis Search 索引名称
     * @param prefix 向量 JSON 记录的 Redis Key 前缀
     * @return 注册文档版本过滤和引用展示所需元数据字段的向量存储
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public VectorStore knowledgeRedisVectorStore(EmbeddingModel embeddingModel,
                                                  JedisConnectionFactory connectionFactory,
                                                  @Value("${spring.ai.vectorstore.redis.index-name}") String indexName,
                                                  @Value("${spring.ai.vectorstore.redis.prefix}") String prefix) {
        return RedisVectorStore.builder(redisClient(connectionFactory), embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("documentId"),
                        RedisVectorStore.MetadataField.tag("chunkId"),
                        RedisVectorStore.MetadataField.tag("logicalDocumentCode"),
                        RedisVectorStore.MetadataField.tag("title"),
                        RedisVectorStore.MetadataField.tag("version"))
                .initializeSchema(true)
                .build();
    }

    /**
     * 创建 Redis Stack 向量适配器。
     *
     * @param vectorStore Spring AI 配置的 Redis 向量存储
     * @return 真实向量存储实现
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public KnowledgeVectorStore redisKnowledgeVectorStore(VectorStore vectorStore, ApplicationMetrics applicationMetrics) {
        return new RedisKnowledgeVectorStore(vectorStore, applicationMetrics);
    }

    /**
     * 将 Spring Data Redis 的连接参数转换为 Spring AI Redis Vector Store 所需的 Jedis 客户端。
     *
     * @param connectionFactory 已注入密码、超时和 TLS 配置的连接工厂
     * @return 与常规 Redis 访问使用相同连接参数的 Jedis 客户端
     */
    private RedisClient redisClient(JedisConnectionFactory connectionFactory) {
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .ssl(connectionFactory.isUseSsl())
                .clientName(connectionFactory.getClientName())
                .timeoutMillis(connectionFactory.getTimeout())
                .password(connectionFactory.getPassword())
                .build();
        return RedisClient.builder()
                .hostAndPort(connectionFactory.getHostName(), connectionFactory.getPort())
                .clientConfig(clientConfig)
                .build();
    }

    /**
     * 创建仅发送无工具 RAG Prompt 的 OpenAI 聊天适配器。
     *
     * @param chatModel Spring AI 自动配置的聊天模型
     * @return 真实聊天适配器
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public RagChatClient springAiRagChatClient(ChatModel chatModel, ApplicationMetrics applicationMetrics) {
        return new SpringAiRagChatClient(chatModel, applicationMetrics);
    }

    /**
     * 创建支持受控工具调用的 Agent 模型适配器；仅在已显式启用真实 RAG/模型时装配。
     *
     * @param chatModel Spring AI 聊天模型
     * @return Agent 模型适配器
     */
    @Bean
    @ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
    public AgentModelClient springAiAgentModelClient(ChatModel chatModel, ApplicationMetrics applicationMetrics) {
        return new SpringAiAgentModelClient(chatModel, applicationMetrics);
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

    /**
     * 在模型未配置时阻止聊天请求伪装为成功。
     *
     * @return 明确失败的 Agent 适配器
     */
    @Bean
    @ConditionalOnMissingBean(AgentModelClient.class)
    public AgentModelClient unavailableAgentModelClient() {
        return new UnavailableAgentModelClient();
    }
}
