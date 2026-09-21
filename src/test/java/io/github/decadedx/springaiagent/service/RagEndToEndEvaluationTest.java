package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.dto.KnowledgeDocumentUploadDTO;
import io.github.decadedx.springaiagent.dto.KnowledgeQuestionDTO;
import io.github.decadedx.springaiagent.enums.KnowledgeIndexStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import io.github.decadedx.springaiagent.mapper.KnowledgeDocumentMapper;
import io.github.decadedx.springaiagent.security.AuthenticatedUser;
import io.github.decadedx.springaiagent.vo.KnowledgeDocumentVO;
import io.github.decadedx.springaiagent.vo.RagAnswerVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 使用真实 Redis Stack 与 OpenAI 对 6 篇、超过 8,000 汉字和 30 题的固定语料执行离线 RAG 质量评估。
 */
@Tag("e2e")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "RUN_RAG_E2E", matches = "true")
@SpringBootTest(properties = {
        "knowledge.rag-enabled=true",
        "knowledge.storage-path=target/rag-e2e-uploads",
        "spring.ai.model.chat=openai",
        "spring.ai.model.embedding=openai",
        "spring.ai.vectorstore.type=redis",
        "spring.ai.vectorstore.redis.initialize-schema=true"
})
class RagEndToEndEvaluationTest {

    private static final List<EvaluationDocument> DOCUMENTS = List.of(
            document("RAG-E2E-A301", "A301 使用规范", "A301 可用于基础编程课程，开放时间为工作日九点至二十一点。"),
            document("RAG-E2E-B402", "B402 安全规范", "B402 包含 GPU 设备，预约人员必须完成安全培训，禁止自行拆装电源。"),
            document("RAG-E2E-C205", "C205 仪器规范", "C205 的示波器使用后必须关闭电源并填写设备使用记录。"),
            document("RAG-E2E-NET", "网络安全规范", "实验室网络仅可用于教学科研，禁止扫描、攻击或绕过访问控制。"),
            document("RAG-E2E-FIRE", "消防应急规范", "发现冒烟或焦糊味时应立即停止使用设备，切断安全电源并联系管理员。"),
            document("RAG-E2E-CLEAN", "环境卫生规范", "离开实验室前应带走个人物品，将桌面恢复整洁并关闭不再使用的显示器。"));

    private static final List<EvaluationCase> CASES = evaluationCases();

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Container
    static final GenericContainer<?> REDIS_STACK = new GenericContainer<>("redis/redis-stack-server:latest")
            .withExposedPorts(6379);

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RagService ragService;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS_STACK::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_STACK.getMappedPort(6379));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @Test
    void shouldReportRetrievalCitationAndRefusalMetricsForFixedCorpus() {
        assertThat(DOCUMENTS.stream().mapToInt(document -> document.content().length()).sum()).isGreaterThanOrEqualTo(8000);
        assertThat(CASES).hasSize(30);
        authenticate(UserRole.ADMIN);
        for (EvaluationDocument document : DOCUMENTS) {
            KnowledgeDocumentVO uploaded = knowledgeService.upload(new KnowledgeDocumentUploadDTO(
                    new MockMultipartFile("file", document.logicalCode() + ".md", "text/markdown",
                            document.content().getBytes(StandardCharsets.UTF_8)), document.logicalCode(), document.title(),
                    "v1", OffsetDateTime.parse("2026-09-21T09:00:00+08:00"), List.of()));
            waitForIndex(uploaded.id());
            knowledgeService.publish(uploaded.id());
        }

        authenticate(UserRole.STUDENT);
        List<Long> durations = new ArrayList<>();
        int answerHits = 0;
        int citationHits = 0;
        int correctRefusals = 0;
        for (EvaluationCase evaluationCase : CASES) {
            long startedAt = System.nanoTime();
            RagAnswerVO answer = ragService.ask(new KnowledgeQuestionDTO(evaluationCase.question()));
            durations.add((System.nanoTime() - startedAt) / 1_000_000L);
            if (evaluationCase.refusal()) {
                if ("当前知识库中没有足够信息".equals(answer.answer())) {
                    correctRefusals++;
                }
            } else if (answer.answer().contains(evaluationCase.expectedKeyword())) {
                answerHits++;
                if (!answer.citations().isEmpty()) {
                    citationHits++;
                }
            }
        }
        durations.sort(Long::compareTo);
        int answeredCases = CASES.size() - 6;
        System.out.printf("RAG E2E: hitRate=%.3f, citationAccuracy=%.3f, refusalRate=%.3f, p50=%dms, p95=%dms%n",
                answerHits / (double) answeredCases, citationHits / (double) Math.max(answerHits, 1),
                correctRefusals / 6.0D, percentile(durations, 0.50D), percentile(durations, 0.95D));
        assertThat(citationHits).isLessThanOrEqualTo(answerHits);
    }

    private void waitForIndex(Long documentId) {
        for (int attempt = 0; attempt < 300; attempt++) {
            if (knowledgeDocumentMapper.selectById(documentId).getIndexStatus() == KnowledgeIndexStatus.SUCCEEDED) {
                return;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待真实向量索引时被中断", exception);
            }
        }
        assertThat(knowledgeDocumentMapper.selectById(documentId).getIndexStatus())
                .isEqualTo(KnowledgeIndexStatus.SUCCEEDED);
    }

    private void authenticate(UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(role == UserRole.ADMIN ? 3L : 1L, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private static EvaluationDocument document(String logicalCode, String title, String rule) {
        String content = (title + "。" + rule + " 本资料只说明固定管理规则，不包含个人预约、实时空闲或工单状态。\n").repeat(85);
        return new EvaluationDocument(logicalCode, title, content);
    }

    private static List<EvaluationCase> evaluationCases() {
        List<EvaluationCase> cases = new ArrayList<>();
        for (EvaluationDocument document : DOCUMENTS) {
            String keyword = switch (document.logicalCode()) {
                case "RAG-E2E-A301" -> "基础编程";
                case "RAG-E2E-B402" -> "安全培训";
                case "RAG-E2E-C205" -> "设备使用记录";
                case "RAG-E2E-NET" -> "访问控制";
                case "RAG-E2E-FIRE" -> "联系管理员";
                default -> "个人物品";
            };
            for (int number = 1; number <= 4; number++) {
                cases.add(new EvaluationCase("请说明" + document.title() + "的第" + number + "项核心要求", keyword, false));
            }
        }
        cases.addAll(List.of(
                new EvaluationCase("我今天还有几笔预约？", "", true),
                new EvaluationCase("我的报修是否已处理？", "", true),
                new EvaluationCase("现在 B402 是否空闲？", "", true),
                new EvaluationCase("请给出系统管理员密码", "", true),
                new EvaluationCase("下周的天气如何？", "", true),
                new EvaluationCase("帮我修改当前预约", "", true)));
        return List.copyOf(cases);
    }

    private static long percentile(List<Long> values, double percentile) {
        return values.get((int) Math.ceil(percentile * values.size()) - 1);
    }

    private record EvaluationDocument(String logicalCode, String title, String content) {
    }

    private record EvaluationCase(String question, String expectedKeyword, boolean refusal) {
    }
}
