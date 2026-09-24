package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.ReservationStatus;
import io.github.decadedx.springaiagent.dto.ReservationCancelPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证浏览器消费的预约主键不会因 JavaScript Number 精度限制而改变。
 */
class ReservationVOTest {

    /**
     * 超出 JavaScript 安全整数范围的预约主键必须序列化为字符串。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeReservationIdAsString() throws Exception {
        long reservationId = 2_102_688_050_821_632_001L;
        ReservationVO reservation = new ReservationVO(reservationId, "RSV-" + reservationId, "LAB-B402",
                "人工智能实验室", null, null, 3, ReservationStatus.CONFIRMED, null);

        String json = new ObjectMapper().writeValueAsString(reservation);

        assertThat(json).contains("\"id\":\"2102688050821632001\"");
    }

    /**
     * 取消草案的预约主键同样必须以字符串返回给浏览器。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeCancellationDraftIdAsString() throws Exception {
        String json = new ObjectMapper().writeValueAsString(new ReservationCancelPayload(2_102_688_050_821_632_001L));

        assertThat(json).contains("\"reservationId\":\"2102688050821632001\"");
    }
}
