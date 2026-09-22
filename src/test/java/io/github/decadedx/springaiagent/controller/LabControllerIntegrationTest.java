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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, LabControllerIntegrationTest.FixedClockConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
class LabControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSearchSeededLabsByEquipmentAndCapacity() throws Exception {
        mockMvc.perform(get("/api/labs")
                        .header("Authorization", "Bearer " + loginAndReadToken())
                        .param("equipment", "GPU")
                        .param("minCapacity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value("LAB-B402"))
                .andExpect(jsonPath("$.data.items[0].openTime").value("09:00"));
    }

    @Test
    void shouldReturnShanghaiHourSlotsForAvailability() throws Exception {
        mockMvc.perform(get("/api/labs/LAB-B402/availability")
                        .header("Authorization", "Bearer " + loginAndReadToken())
                        .param("date", "2026-09-21")
                        .param("from", "10:00")
                        .param("to", "12:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labId").value("LAB-B402"))
                .andExpect(jsonPath("$.data.openTime").value("09:00"))
                .andExpect(jsonPath("$.data.closeTime").value("21:00"))
                .andExpect(jsonPath("$.data.slots", hasSize(2)))
                .andExpect(jsonPath("$.data.slots[0].startTime").value("2026-09-21T10:00:00+08:00"))
                .andExpect(jsonPath("$.data.slots[0].available").value(true));
    }

    @Test
    void shouldReturnLabNotFoundForUnknownAvailabilityLab() throws Exception {
        mockMvc.perform(get("/api/labs/LAB-NOT-FOUND/availability")
                        .header("Authorization", "Bearer " + loginAndReadToken())
                        .param("date", "2026-09-21"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.requestId").value(notNullValue()));
    }

    @Test
    void shouldDeclarePatternMatchingSerializedLabTimes() throws Exception {
        String openApi = Files.readString(Path.of("docs/openapi.yaml"));
        Matcher openTimeMatcher = Pattern.compile("openTime: \\{ type: string, pattern: '([^']+)' \\}")
                .matcher(openApi);
        Matcher closeTimeMatcher = Pattern.compile("closeTime: \\{ type: string, pattern: '([^']+)' \\}")
                .matcher(openApi);

        assertTrue(openTimeMatcher.find());
        assertTrue(Pattern.compile(openTimeMatcher.group(1)).matcher("09:00").matches());
        assertTrue(closeTimeMatcher.find());
        assertTrue(Pattern.compile(closeTimeMatcher.group(1)).matcher("22:00").matches());
    }

    private String loginAndReadToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"student01\",\"password\":\"student01\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedBusinessClock() {
            return Clock.fixed(Instant.parse("2026-09-20T08:00:00Z"), TimeConfig.BUSINESS_ZONE);
        }
    }
}
