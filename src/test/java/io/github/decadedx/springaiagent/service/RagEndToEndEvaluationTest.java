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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用真实模型、Redis Stack 和固定三十题比较两组 RAG 参数并保存原始证据。 */
@Tag("e2e")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "RUN_RAG_E2E", matches = "true")
@SpringBootTest(properties = {"knowledge.rag-enabled=true", "knowledge.storage-path=target/rag-e2e-uploads",
        "spring.ai.model.chat=openai", "spring.ai.model.embedding=openai", "spring.ai.vectorstore.type=redis",
        "spring.ai.vectorstore.redis.initialize-schema=true", "spring.ai.vectorstore.redis.index-name=rag-e2e-index",
        "spring.ai.vectorstore.redis.prefix=rag-e2e:",
        "spring.ai.openai.chat.base-url=${OPENAI_BASE_URL}",
        "spring.ai.openai.chat.api-key=${OPENAI_API_KEY}",
        "spring.ai.openai.chat.model=${OPENAI_CHAT_MODEL}",
        "spring.ai.openai.chat.options.model=${OPENAI_CHAT_MODEL}",
        "spring.ai.openai.embedding.base-url=${DASHSCOPE_EMBEDDING_BASE_URL}",
        "spring.ai.openai.embedding.api-key=${DASHSCOPE_API_KEY}",
        "spring.ai.openai.embedding.model=${DASHSCOPE_EMBEDDING_MODEL}",
        "spring.ai.openai.embedding.dimensions=${DASHSCOPE_EMBEDDING_DIMENSIONS}"})
@Import(RagEndToEndEvaluationTest.RedisTestConfiguration.class)
class RagEndToEndEvaluationTest {

    private static final String INSUFFICIENT_KNOWLEDGE = "当前知识库中没有足够信息";
    private static final List<SourceDocument> DOCUMENTS = List.of(
            new SourceDocument("RULES", "v1.0", "实验室预约管理办法", "01-实验室预约管理办法.md"),
            new SourceDocument("AI_GUIDE", "v1.0", "人工智能实验室使用指南", "02-人工智能实验室使用指南.md"),
            new SourceDocument("EMBEDDED_GUIDE", "v1.0", "嵌入式实验室安全规范", "03-嵌入式实验室安全规范.md"),
            new SourceDocument("REPAIR_GUIDE", "v1.0", "设备报修操作指南", "04-设备报修操作指南.md"),
            new SourceDocument("A301_GUIDE", "v1.0", "软件工程实验室使用指南", "05-软件工程实验室使用指南.md"),
            new SourceDocument("FAQ", "v1.0", "校园实验室智能服务平台常见问题汇编", "06-常见问题汇编.md"));

    @Container static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");
    @Container static final GenericContainer<?> REDIS_STACK = new GenericContainer<>("redis/redis-stack:latest").withExposedPorts(6379);

    @Autowired private KnowledgeService knowledgeService;
    @Autowired private KnowledgeDocumentMapper knowledgeDocumentMapper;
    @Autowired private KnowledgeVectorStore knowledgeVectorStore;
    @Autowired private RagChatClient ragChatClient;
    @Autowired private RagService ragService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Value("${rag.evaluation.label:unknown}") private String evaluationLabel;
    @Value("${knowledge.rag.chunk-size}") private int chunkSize;
    @Value("${knowledge.rag.chunk-overlap}") private int chunkOverlap;
    @Value("${knowledge.rag.min-boundary-chunk-size}") private int minBoundaryChunkSize;
    @Value("${knowledge.rag.top-k}") private int topK;
    @Value("${knowledge.rag.similarity-threshold}") private double similarityThreshold;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS_STACK::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_STACK.getMappedPort(6379));
    }

    /** 为 Redis Vector Store 提供 Jedis 连接，隔离于应用默认 Redis 客户端实现。 */
    @TestConfiguration
    static class RedisTestConfiguration {

        /**
         * 创建指向测试容器的 Jedis 连接工厂。
         *
         * @return 测试专用 Redis 连接工厂
         */
        @Bean
        JedisConnectionFactory jedisConnectionFactory() {
            return new JedisConnectionFactory(new RedisStandaloneConfiguration(
                    REDIS_STACK.getHost(), REDIS_STACK.getMappedPort(6379)));
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    /** 索引真实资料，执行固定题集并写出每题的检索、回答、引用与耗时证据。 */
    @Test
    void shouldWriteEvidenceForFixedThirtyQuestionEvaluation() throws Exception {
        List<EvaluationCase> cases = readCases();
        assertThat(cases).hasSize(30);
        assertThat(ragChatClient.complete("仅返回 READY。")).isNotBlank();
        authenticate(UserRole.ADMIN);
        publishHistoricalVersion();
        for (SourceDocument document : DOCUMENTS) {
            KnowledgeDocumentVO uploaded = upload(document);
            waitForIndex(uploaded.id());
            knowledgeService.publish(uploaded.id());
        }
        authenticate(UserRole.STUDENT);
        List<Long> ids = knowledgeDocumentMapper.selectPublishedSucceededIds();
        List<Map<String, Object>> results = new ArrayList<>();
        for (EvaluationCase item : cases) {
            long startedAt = System.nanoTime();
            List<KnowledgeVectorDocument> matches = knowledgeVectorStore.search(item.question(), ids, topK, similarityThreshold);
            RagAnswerVO answer = ragService.ask(new KnowledgeQuestionDTO(item.question()));
            results.add(result(item, matches, answer, Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L)));
        }
        writeResult(cases, results);
        assertThat(results).hasSize(30);
    }

    /** 发布随后被当前版本替换的旧规则，用于验证停用版本不会参与检索。 */
    private void publishHistoricalVersion() {
        byte[] content = "旧版规则：B402 周末开放，草案有效三十分钟，未培训学生可以短时使用 GPU。".repeat(30).getBytes(StandardCharsets.UTF_8);
        KnowledgeDocumentVO historical = knowledgeService.upload(new KnowledgeDocumentUploadDTO(
                new MockMultipartFile("file", "historical.md", "text/markdown", content), "RULES", "实验室预约管理办法", "v0.0",
                OffsetDateTime.parse("2026-09-21T09:00:00+08:00"), List.of()));
        waitForIndex(historical.id());
        knowledgeService.publish(historical.id());
    }

    /** 上传一篇项目真实知识文档。 */
    private KnowledgeDocumentVO upload(SourceDocument document) throws Exception {
        return knowledgeService.upload(new KnowledgeDocumentUploadDTO(new MockMultipartFile("file", document.fileName(), "text/markdown",
                Files.readAllBytes(Path.of("knowledge", "knowledge-base", document.fileName()))), document.logicalCode(), document.title(),
                document.version(), OffsetDateTime.parse("2026-09-21T09:00:00+08:00"), List.of()));
    }

    /** 等待异步索引任务成功。 */
    private void waitForIndex(Long documentId) {
        for (int attempt = 0; attempt < 600; attempt++) {
            if (knowledgeDocumentMapper.selectById(documentId).getIndexStatus() == KnowledgeIndexStatus.SUCCEEDED) return;
            try { Thread.sleep(100L); } catch (InterruptedException exception) {
                Thread.currentThread().interrupt(); throw new AssertionError("等待真实向量索引时被中断", exception);
            }
        }
        assertThat(knowledgeDocumentMapper.selectById(documentId).getIndexStatus()).isEqualTo(KnowledgeIndexStatus.SUCCEEDED);
    }

    /** 从固定 JSON 题集读取测试案例。 */
    private List<EvaluationCase> readCases() throws Exception {
        JsonNode root = objectMapper.readTree(Files.readString(Path.of("tests", "rag-evaluation", "questions.json")));
        List<EvaluationCase> cases = new ArrayList<>();
        for (JsonNode node : root) cases.add(new EvaluationCase(node.path("id").asText(), node.path("category").asText(),
                node.path("question").asText(), strings(node.path("expectedEvidence")), node.path("shouldRefuse").asBoolean()));
        return cases;
    }

    /** 读取 JSON 字符串数组。 */
    private List<String> strings(JsonNode node) {
        List<String> values = new ArrayList<>(); node.forEach(value -> values.add(value.asText())); return values;
    }

    /** 组装单题可复核原始结果。 */
    private Map<String, Object> result(EvaluationCase item, List<KnowledgeVectorDocument> matches, RagAnswerVO answer, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.id()); result.put("category", item.category()); result.put("question", item.question());
        result.put("topK", matches.stream().map(match -> Map.of("chunkId", match.metadata().get("chunkId"), "title", match.metadata().get("title"),
                "version", match.metadata().get("version"), "content", match.content())).toList());
        result.put("answer", answer.answer()); result.put("citations", answer.citations()); result.put("elapsedMs", elapsedMs);
        List<String> titles = item.expectedEvidence().stream().map(value -> value.replace("当前发布版：", "").split("#", 2)[0]).toList();
        boolean evidenceHit = titles.stream().allMatch(title -> matches.stream().anyMatch(match -> title.equals(match.metadata().get("title"))));
        result.put("retrievalHit", evidenceHit);
        result.put("crossDocumentCoverage", "cross_document".equals(item.category()) && evidenceHit);
        result.put("refusalCorrect", item.shouldRefuse() && INSUFFICIENT_KNOWLEDGE.equals(answer.answer()));
        result.put("citationAccurate", answer.citations().stream().allMatch(citation -> matches.stream().anyMatch(match ->
                citation.chunkId().equals(match.metadata().get("chunkId")) && match.content().contains(citation.excerpt()))));
        return result;
    }

    /** 写入逐题结果和由真实输出计算的指标。 */
    private void writeResult(List<EvaluationCase> cases, List<Map<String, Object>> results) throws Exception {
        List<Long> durations = results.stream().map(value -> (Long) value.get("elapsedMs")).sorted().toList();
        Map<String, Object> summary = Map.of("caseCount", cases.size(), "retrievalHitRate", ratio(results, "retrievalHit", 25),
                "crossDocumentCoverage", ratio(results, "crossDocumentCoverage", 5), "citationAccuracy", ratio(results, "citationAccurate", 30),
                "refusalRate", ratio(results, "refusalCorrect", 5), "p50Ms", percentile(durations, .50D), "p95Ms", percentile(durations, .95D));
        Map<String, Object> report = Map.of("parameters", Map.of("chunkSize", chunkSize, "chunkOverlap", chunkOverlap,
                "minBoundaryChunkSize", minBoundaryChunkSize, "topK", topK, "similarityThreshold", similarityThreshold), "summary", summary, "results", results);
        Path output = Path.of("tests", "rag-evaluation", "results", evaluationLabel + ".json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    /** 计算布尔指标比率。 */
    private double ratio(List<Map<String, Object>> results, String field, int denominator) {
        return results.stream().filter(value -> Boolean.TRUE.equals(value.get(field))).count() / (double) denominator;
    }

    /** 计算已排序耗时的百分位。 */
    private long percentile(List<Long> values, double percentile) { return values.get((int) Math.ceil(percentile * values.size()) - 1); }

    /** 设置当前线程的管理员或学生认证身份。 */
    private void authenticate(UserRole role) {
        AuthenticatedUser user = new AuthenticatedUser(role == UserRole.ADMIN ? 3L : 1L, role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private record SourceDocument(String logicalCode, String version, String title, String fileName) { }
    private record EvaluationCase(String id, String category, String question, List<String> expectedEvidence, boolean shouldRefuse) { }
}
