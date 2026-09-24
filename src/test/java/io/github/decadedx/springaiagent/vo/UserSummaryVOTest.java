package io.github.decadedx.springaiagent.vo;

import io.github.decadedx.springaiagent.enums.TrainingStatus;
import io.github.decadedx.springaiagent.enums.UserRole;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证登录响应中的用户主键不会因 JavaScript Number 精度限制而改变。
 */
class UserSummaryVOTest {

    /**
     * 超出 JavaScript 安全整数范围的用户主键必须序列化为字符串。
     *
     * @throws Exception JSON 序列化失败时测试失败
     */
    @Test
    void shouldSerializeUserIdAsString() throws Exception {
        UserSummaryVO user = new UserSummaryVO(2_102_688_050_821_632_001L, "student01", UserRole.STUDENT,
                TrainingStatus.PASSED);

        String json = new ObjectMapper().writeValueAsString(user);

        assertThat(json).contains("\"id\":\"2102688050821632001\"");
    }
}
