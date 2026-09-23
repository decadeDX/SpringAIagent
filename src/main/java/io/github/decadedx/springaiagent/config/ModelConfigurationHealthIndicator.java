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

    /** OpenAI 密钥，仅用于判断是否已配置，绝不写入健康响应。 */
    private final String apiKey;

    /** 聊天模型标识。 */
    private final String chatModel;

    /** 嵌入模型标识。 */
    private final String embeddingModel;

    /**
     * 创建模型配置健康检查。
     *
     * @param apiKey OpenAI 密钥
     * @param chatModel 聊天模型标识
     * @param embeddingModel 嵌入模型标识
     */
    public ModelConfigurationHealthIndicator(@Value("${spring.ai.openai.api-key:}") String apiKey,
                                             @Value("${spring.ai.openai.chat.model:}") String chatModel,
                                             @Value("${spring.ai.openai.embedding.model:}") String embeddingModel) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    /** {@inheritDoc} */
    @Override
    public Health health() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(chatModel) && StringUtils.hasText(embeddingModel)
                ? Health.up().build() : Health.down().build();
    }
}
