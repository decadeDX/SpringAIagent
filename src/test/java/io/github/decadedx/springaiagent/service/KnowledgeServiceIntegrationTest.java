package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.common.ApiCode;
import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.entity.KnowledgeDocument;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.KnowledgePublishStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.exception.BusinessException;
import io.github.decadedx.springaiagent.mapper.KnowledgeChunkMapper;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 使用可控 fake 向量库验证索引状态、发布互斥和停用后的检索资格变化。
 */
@Import({TestcontainersConfiguration.class, KnowledgeServiceIntegrationTest.FakeVectorStoreConfiguration.class})
@SpringBootTest(properties = "knowledge.storage-path=target/test-knowledge-service")
class KnowledgeServiceIntegrationTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private KnowledgeIndexService knowledgeIndexService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeChunkMapper knowledgeChunkMapper;

    @Autowired
    private FakeKnowledgeVectorStore fakeKnowledgeVectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
        fakeKnowledgeVectorStore.failWrites = false;
        authenticateAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @Test
    void shouldIndexPublishReplacePreviousVersionAndDisableItFromRetrieval() {
        KnowledgeDocumentVO first = upload("RULES", "v1", "预约前必须完成安全培训。".repeat(100));
        index(first.id());
        KnowledgeDocumentVO firstPublished = knowledgeService.publish(first.id());
        KnowledgeDocumentVO second = upload("RULES", "v2", "预约时请携带校园卡。".repeat(100));
        index(second.id());
        KnowledgeDocumentVO secondPublished = knowledgeService.publish(second.id());

        assertThat(firstPublished.publishStatus()).isEqualTo(KnowledgePublishStatus.PUBLISHED);
        assertThat(secondPublished.publishStatus()).isEqualTo(KnowledgePublishStatus.PUBLISHED);
        assertThat(knowledgeDocumentMapper.selectById(first.id()).getPublishStatus())
                .isEqualTo(KnowledgePublishStatus.DISABLED);
        assertThat(knowledgeDocumentMapper.selectPublishedSucceededIds()).containsExactly(second.id());

        KnowledgeDocumentVO disabled = knowledgeService.disable(second.id());
        assertThat(disabled.publishStatus()).isEqualTo(KnowledgePublishStatus.DISABLED);
        assertThat(knowledgeDocumentMapper.selectPublishedSucceededIds()).isEmpty();
    }

    @Test
    void shouldRecordFailedIndexAndRejectPublishingHalfFinishedVersion() {
        fakeKnowledgeVectorStore.failWrites = true;
        KnowledgeDocumentVO document = upload("FAILED", "v1", "安全要求。".repeat(100));

        knowledgeIndexService.schedule(document.id());
        waitForIndexStatus(document.id(), KnowledgeIndexStatus.FAILED);

        KnowledgeDocument failed = knowledgeDocumentMapper.selectById(document.id());
        assertThat(failed.getIndexStatus()).isEqualTo(KnowledgeIndexStatus.FAILED);
        assertThat(knowledgeChunkMapper.countByDocumentId(document.id())).isZero();
        assertThatThrownBy(() -> knowledgeService.publish(document.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.BUSINESS_CONFLICT));
    }

    private KnowledgeDocumentVO upload(String logicalCode, String version, String content) {
        return knowledgeService.upload(new KnowledgeDocumentUploadDTO(
                new MockMultipartFile("file", logicalCode + ".md", "text/markdown",
                        content.getBytes(StandardCharsets.UTF_8)),
                logicalCode, "实验室指南", version, OffsetDateTime.parse("2026-09-21T09:00:00+08:00"), List.of()));
    }

    private void index(Long documentId) {
        knowledgeIndexService.schedule(documentId);
        waitForIndexStatus(documentId, KnowledgeIndexStatus.SUCCEEDED);
    }

    private void waitForIndexStatus(Long documentId, KnowledgeIndexStatus expectedStatus) {
        for (int attempt = 0; attempt < 250; attempt++) {
            if (knowledgeDocumentMapper.selectById(documentId).getIndexStatus() == expectedStatus) {
                return;
            }
            try {
                Thread.sleep(20L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待异步索引时被中断", exception);
            }
        }
        assertThat(knowledgeDocumentMapper.selectById(documentId).getIndexStatus()).isEqualTo(expectedStatus);
    }

    private void authenticateAdmin() {
        AuthenticatedUser user = new AuthenticatedUser(3L, UserRole.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @TestConfiguration
    static class FakeVectorStoreConfiguration {

        @Bean
        @Primary
        FakeKnowledgeVectorStore fakeKnowledgeVectorStore() {
            return new FakeKnowledgeVectorStore();
        }
    }

    static class FakeKnowledgeVectorStore implements KnowledgeVectorStore {

        private boolean failWrites;

        @Override
        public void add(List<KnowledgeVectorDocument> documents) {
            if (failWrites) {
                throw new IllegalStateException("fake vector write failure");
            }
        }

        @Override
        public void delete(Collection<String> vectorRecordIds) {
        }

        @Override
        public List<KnowledgeVectorDocument> search(String question, Collection<Long> documentIds, int topK,
                                                    double similarityThreshold) {
            return List.of();
        }
    }
}
