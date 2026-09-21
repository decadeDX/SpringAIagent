package io.github.decadedx.springaiagent.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 覆盖知识管理接口的认证边界、上传响应和独立 RAG 固定拒答响应。
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "knowledge.storage-path=target/test-knowledge-web")
@AutoConfigureMockMvc
class KnowledgeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM knowledge_chunk");
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @Test
    void shouldRejectStudentAndAcceptAdminKnowledgeUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "safety.md", "text/markdown",
                "# 安全规则\n预约前必须完成安全培训。".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/admin/knowledge/documents")
                        .file(file)
                        .param("logicalDocumentCode", "SAFETY-GUIDE")
                        .param("title", "安全指南")
                        .param("version", "v1")
                        .param("effectiveAt", "2026-09-21T09:00:00+08:00")
                        .header("Authorization", "Bearer " + login("student01")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(multipart("/api/admin/knowledge/documents")
                        .file(file)
                        .param("logicalDocumentCode", "SAFETY-GUIDE")
                        .param("title", "安全指南")
                        .param("version", "v1")
                        .param("effectiveAt", "2026-09-21T09:00:00+08:00")
                        .header("Authorization", "Bearer " + login("admin01")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publishStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.indexStatus").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundForUnknownDocumentAndStandardFixedRagRefusal() throws Exception {
        String adminToken = login("admin01");
        mockMvc.perform(post("/api/admin/knowledge/documents/{documentVersionId}/publish", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));

        mockMvc.perform(post("/api/knowledge/questions")
                        .header("Authorization", "Bearer " + login("student01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"实验室预约需要什么条件？\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("当前知识库中没有足够信息"))
                .andExpect(jsonPath("$.data.citations").isEmpty())
                .andExpect(jsonPath("$.data.retrieval.topK").value(5));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
