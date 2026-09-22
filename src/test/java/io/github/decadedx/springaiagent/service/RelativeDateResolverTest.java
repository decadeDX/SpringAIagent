package io.github.decadedx.springaiagent.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证相对日期始终以服务端上海时区而非模型推测为准。
 */
class RelativeDateResolverTest {

    /**
     * 明天、后天和下周星期应转换为可展示的明确日期。
     */
    @Test
    void resolvesSupportedChineseRelativeDates() {
        RelativeDateResolver resolver = new RelativeDateResolver(Clock.fixed(
                Instant.parse("2026-09-22T04:00:00Z"), ZoneId.of("Asia/Shanghai")));

        assertThat(resolver.resolve("明天或后天下周三预约")).containsExactlyInAnyOrder(
                "明天=2026-09-23", "后天=2026-09-24", "下周三=2026-09-30");
    }
}
