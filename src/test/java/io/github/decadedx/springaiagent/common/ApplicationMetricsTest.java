package io.github.decadedx.springaiagent.common;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证核心业务分支能写入稳定的 Micrometer 指标。
 */
class ApplicationMetricsTest {

    /** 所有离散业务事件应各自递增一次。 */
    @Test
    void recordsBusinessEventCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApplicationMetrics metrics = new ApplicationMetrics(registry);

        metrics.reservationConflict();
        metrics.actionIdempotentReplay();
        metrics.ragInsufficientEvidence();
        metrics.agentToolFailure();
        metrics.chatRateLimited();

        assertThat(registry.get("lab.reservation.conflicts").counter().count()).isEqualTo(1D);
        assertThat(registry.get("lab.action.idempotent.replays").counter().count()).isEqualTo(1D);
        assertThat(registry.get("lab.rag.insufficient_evidence").counter().count()).isEqualTo(1D);
        assertThat(registry.get("lab.agent.tool.failures").counter().count()).isEqualTo(1D);
        assertThat(registry.get("lab.chat.rate_limited").counter().count()).isEqualTo(1D);
    }
}
