package io.github.decadedx.springaiagent.vo;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证审计记录主键不会因 JavaScript Number 精度限制而改变。
 */
class AgentTraceVOTest {

    /**
     * 超出 JavaScript 安全整数范围的审计记录主键必须序列化为字符串。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeTraceIdAsString() throws Exception {
        AgentTraceVO trace = new AgentTraceVO(2_102_688_050_821_632_001L, "req-01", "session-01",
                "prepareReservation", "成功", 10, null, null);

        String json = new ObjectMapper().writeValueAsString(trace);

        assertThat(json).contains("\"id\":\"2102688050821632001\"");
    }
}
