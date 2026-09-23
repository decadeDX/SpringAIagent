package io.github.decadedx.springaiagent.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "security.cors.allowed-origins=http://localhost:5173")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** 用于隔离每个用例的 Redis 单账号会话。 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 每个用例开始前仅清理认证会话键，不影响其他 Redis 测试数据。 */
    @BeforeEach
    void clearUserSessions() {
        Set<String> sessionKeys = stringRedisTemplate.keys("auth:session:*");
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            stringRedisTemplate.delete(sessionKeys);
        }
    }

    @Test
    void shouldLoginSeededStudentWithoutReturningPasswordHash() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Request-Id", "req-login-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"student01\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-login-001"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.requestId").value("req-login-001"))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("student01"))
                .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldRejectWrongPasswordWithUnifiedUnauthorizedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.requestId").value(notNullValue()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldRejectSecondLoginForAnActiveAccount() throws Exception {
        loginAndReadToken("student01", "student01");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"student01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40905));
    }

    @Test
    void shouldAllowLoginAfterCurrentSessionLogsOut() throws Exception {
        String token = loginAndReadToken("student01", "student01");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"student01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRequestsWhoseRedisSessionIsMissing() throws Exception {
        String token = loginAndReadToken("student01", "student01");
        stringRedisTemplate.delete("auth:session:1");

        mockMvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(get("/api/reservations/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void shouldRequireTokenForBusinessApi() throws Exception {
        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.requestId").value(notNullValue()));
    }

    @Test
    void shouldRejectStudentFromAdminApi() throws Exception {
        String token = loginAndReadToken("student01", "student01");

        mockMvc.perform(patch("/api/admin/labs/LAB-A301")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.requestId").value(notNullValue()));
    }

    @Test
    void shouldRejectInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/reservations/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void shouldReturnUnifiedValidationErrorWithRequestId() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Request-Id", "req-invalid-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "req-invalid-login"))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.requestId").value("req-invalid-login"));
    }

    @Test
    void shouldOnlyAllowConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/labs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/labs")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotAllowAnotherUserToReadChatSession() throws Exception {
        String ownerToken = loginAndReadToken("student01", "student01");
        String sessionId = JsonPath.read(mockMvc.perform(post("/api/chat/sessions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.sessionId");

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId)
                        .header("Authorization", "Bearer " + loginAndReadToken("student02", "student02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"查询我的预约\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400))
                .andExpect(jsonPath("$.requestId").value(notNullValue()));
    }

    private String loginAndReadToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
