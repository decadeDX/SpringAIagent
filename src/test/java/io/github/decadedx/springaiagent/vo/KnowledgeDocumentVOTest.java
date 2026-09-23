package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证浏览器消费的文档版本主键不会因 JavaScript Number 精度限制而改变。
 */
class KnowledgeDocumentVOTest {

    /**
     * 超出 JavaScript 安全整数范围的主键必须序列化为字符串。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeDocumentIdAsString() throws Exception {
        long documentId = 9_223_372_036_854_775_000L;
        KnowledgeDocumentVO document = new KnowledgeDocumentVO(documentId, "01", "v1.0", "实验室预约管理办法",
                null, List.of(), KnowledgePublishStatus.DRAFT, KnowledgeIndexStatus.SUCCEEDED, 1, null);

        String json = new ObjectMapper().writeValueAsString(document);

        assertThat(json).contains("\"id\":\"9223372036854775000\"");
    }
}
