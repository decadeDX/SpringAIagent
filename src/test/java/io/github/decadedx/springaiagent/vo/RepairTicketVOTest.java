package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.RepairTicketStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证浏览器消费的工单主键不会因 JavaScript Number 精度限制而改变。
 */
class RepairTicketVOTest {

    /**
     * 超出 JavaScript 安全整数范围的工单主键必须序列化为字符串。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeTicketIdAsString() throws Exception {
        long ticketId = 2_102_688_050_821_632_001L;
        RepairTicketVO ticket = new RepairTicketVO(ticketId, "RPT-" + ticketId, "LAB-B402", "GPU-03",
                "无法启动", false, RepairTicketStatus.SUBMITTED, null, null, null);

        String json = new ObjectMapper().writeValueAsString(ticket);

        assertThat(json).contains("\"id\":\"2102688050821632001\"");
    }
}
