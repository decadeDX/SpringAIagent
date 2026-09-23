package io.github.decadedx.springaiagent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 在显式启用 RAG 时仅验证模型必要配置，不发送会产生费用的探测请求。
 */
@Component("modelConfiguration")
@ConditionalOnProperty(prefix = "knowledge", name = "rag-enabled", havingValue = "true")
public class ModelConfigurationHealthIndicator implements HealthIndicator {

    /** 聊天服务密钥，仅用于判断 StepFun 端点是否已配置，绝不写入健康响应。 */
    private final String chatApiKey;

    /** 嵌入服务密钥，仅用于判断 DashScope 端点是否已配置，绝不写入健康响应。 */
    private final String embeddingApiKey;

    /** 聊天模型标识。 */
    private final String chatModel;

    /** 嵌入模型标识。 */
    private final String embeddingModel;

    /**
     * 创建模型配置健康检查。
     *
     * @param chatApiKey StepFun 聊天服务密钥
     * @param embeddingApiKey DashScope 嵌入服务密钥
     * @param chatModel 聊天模型标识
     * @param embeddingModel 嵌入模型标识
     */
    public ModelConfigurationHealthIndicator(@Value("${spring.ai.openai.api-key:}") String chatApiKey,
                                             @Value("${spring.ai.openai.embedding.api-key:}") String embeddingApiKey,
                                             @Value("${spring.ai.openai.chat.model:}") String chatModel,
                                             @Value("${spring.ai.openai.embedding.model:}") String embeddingModel) {
        this.chatApiKey = chatApiKey;
        this.embeddingApiKey = embeddingApiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    /** {@inheritDoc} */
    @Override
    public Health health() {
        return StringUtils.hasText(chatApiKey) && StringUtils.hasText(embeddingApiKey)
                && StringUtils.hasText(chatModel) && StringUtils.hasText(embeddingModel)
                ? Health.up().build() : Health.down().build();
    }
}
