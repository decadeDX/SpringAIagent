package io.github.decadedx.springaiagent.controller;

import com.jayway.jsonpath.JsonPath;
import io.github.decadedx.springaiagent.TestcontainersConfiguration;
import io.github.decadedx.springaiagent.config.TimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证实验室详情缓存及管理员更新后的提交后失效。
 */
@Import({TestcontainersConfiguration.class, LabCacheIntegrationTest.FixedClockConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class LabCacheIntegrationTest {

    /** HTTP 集成测试客户端。 */
    @Autowired
    private MockMvc mockMvc;

    /** 测试 Redis 连接。 */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 首次详情查询写缓存，管理员更新后缓存必须失效且读取到新状态。
     */
    @Test
    void invalidatesDetailCacheAfterAdminCommitsMaintenanceUpdate() throws Exception {
        mockMvc.perform(get("/api/labs/LAB-A301").header("Authorization", "Bearer " + login("student01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(stringRedisTemplate.hasKey("lab:detail:LAB-A301")).isTrue();

        mockMvc.perform(patch("/api/admin/labs/LAB-A301")
                        .header("Authorization", "Bearer " + login("admin01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"MAINTENANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));
        assertThat(stringRedisTemplate.hasKey("lab:detail:LAB-A301")).isFalse();

        mockMvc.perform(get("/api/labs/LAB-A301").header("Authorization", "Bearer " + login("student01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));
    }

    /**
     * 登录预置账号并读取 Bearer Token。
     *
     * @param username 账号名
     * @return 访问令牌
     */
    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + username + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    /** 固定业务时钟以保持集成测试预约语义稳定。 */
    @TestConfiguration
    static class FixedClockConfiguration {

        /**
         * 提供固定上海时区业务时钟。
         *
         * @return 固定时钟
         */
        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-09-20T08:00:00Z"), TimeConfig.BUSINESS_ZONE);
        }
    }
}
