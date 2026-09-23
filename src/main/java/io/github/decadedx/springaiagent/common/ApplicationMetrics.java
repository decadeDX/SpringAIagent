package io.github.decadedx.springaiagent.common;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 集中记录不含用户内容的核心业务指标，指标只用于受控运维采集。
 */
@Component
public class ApplicationMetrics {

    /** Micrometer 指标注册表。 */
    private final MeterRegistry meterRegistry;

    /**
     * 创建业务指标入口，并预注册当前尚未发生的模型重试计数器。
     *
     * @param meterRegistry 指标注册表
     */
    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        meterRegistry.counter("lab.ai.model.retries");
    }

    /** 记录数据库时隙冲突。 */
    public void reservationConflict() {
        meterRegistry.counter("lab.reservation.conflicts").increment();
    }

    /** 记录已持久化动作的成功幂等重放。 */
    public void actionIdempotentReplay() {
        meterRegistry.counter("lab.action.idempotent.replays").increment();
    }

    /** 记录 RAG 因缺少可信依据而拒答。 */
    public void ragInsufficientEvidence() {
        meterRegistry.counter("lab.rag.insufficient_evidence").increment();
    }

    /** 记录 Agent 工具调用失败。 */
    public void agentToolFailure() {
        meterRegistry.counter("lab.agent.tool.failures").increment();
    }

    /** 记录聊天固定窗口限流命中。 */
    public void chatRateLimited() {
        meterRegistry.counter("lab.chat.rate_limited").increment();
    }

    /**
     * 记录一次模型调用耗时，不包含提示词、返回内容或模型密钥。
     *
     * @param durationNanos 调用耗时纳秒
     */
    public void modelCall(long durationNanos) {
        meterRegistry.timer("lab.ai.model.calls").record(Math.max(0L, durationNanos), java.util.concurrent.TimeUnit.NANOSECONDS);
    }

    /**
     * 记录一次向量库操作耗时；Spring AI 在该调用内完成 Embedding 请求。
     *
     * @param durationNanos 调用耗时纳秒
     */
    public void embeddingOperation(long durationNanos) {
        meterRegistry.timer("lab.ai.embedding.operations").record(Math.max(0L, durationNanos),
                java.util.concurrent.TimeUnit.NANOSECONDS);
    }
}
