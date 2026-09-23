package io.github.decadedx.springaiagent.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证模型健康检查只依据必要配置给出状态。
 */
class ModelConfigurationHealthIndicatorTest {

    /** 缺少密钥时 RAG 模型配置不可用。 */
    @Test
    void reportsDownWhenApiKeyIsMissing() {
        assertThat(new ModelConfigurationHealthIndicator("", "gpt-5-mini", "text-embedding-3-small")
                .health().getStatus().getCode()).isEqualTo("DOWN");
    }

    /** 密钥和模型名完整时不需要真实模型请求即可就绪。 */
    @Test
    void reportsUpWhenRequiredConfigurationIsPresent() {
        assertThat(new ModelConfigurationHealthIndicator("test-key", "gpt-5-mini", "text-embedding-3-small")
                .health().getStatus().getCode()).isEqualTo("UP");
    }
}
