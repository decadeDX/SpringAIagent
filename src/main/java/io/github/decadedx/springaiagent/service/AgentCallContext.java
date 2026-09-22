package io.github.decadedx.springaiagent.service;

import io.github.decadedx.springaiagent.vo.ActionDraftVO;
import io.github.decadedx.springaiagent.vo.KnowledgeCitationVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存一轮同步模型调用中的工具计数、引用和草案；每轮结束后必须清理线程状态。
 */
@Component
public class AgentCallContext {

    /** 单轮允许的真实工具调用上限。 */
    private static final int MAX_TOOL_CALLS = 8;

    /** 当前请求线程的短期上下文。 */
    private final ThreadLocal<State> current = new ThreadLocal<>();

    /**
     * 开始新一轮 Agent 调用。
     *
     * @param sessionId 当前会话
     */
    public void begin(String sessionId) {
        current.set(new State(sessionId));
    }

    /**
     * 在实际调用工具前占用一个额度。
     *
     * @throws ToolLimitReachedException 第九次调用时抛出
     */
    public void acquireToolCall() {
        State state = requireState();
        if (state.toolCallCount >= MAX_TOOL_CALLS) {
            throw new ToolLimitReachedException();
        }
        state.toolCallCount++;
    }

    /**
     * 返回当前调用绑定的会话，不结束本轮上下文。
     *
     * @return 当前会话标识
     */
    public String sessionId() {
        return requireState().sessionId;
    }

    /**
     * 保存工具产生的可信引用。
     *
     * @param citations RAG 工具的实际引用
     */
    public void addCitations(List<KnowledgeCitationVO> citations) {
        State state = requireState();
        for (KnowledgeCitationVO citation : citations) {
            state.citations.putIfAbsent(citation.chunkId(), citation);
        }
    }

    /**
     * 保存本轮最后生成的待确认草案。
     *
     * @param draft 服务端草案
     */
    public void setDraft(ActionDraftVO draft) {
        requireState().draft = draft;
    }

    /**
     * 获取本轮完成结果并清理线程状态。
     *
     * @return 不可变快照
     */
    public Snapshot finish() {
        State state = requireState();
        try {
            return new Snapshot(state.sessionId, state.toolCallCount, List.copyOf(state.citations.values()), state.draft);
        } finally {
            current.remove();
        }
    }

    /**
     * 无论模型是否失败都清理上下文。
     */
    public void clear() {
        current.remove();
    }

    /**
     * 读取当前状态，防止工具被绕过编排器直接调用。
     *
     * @return 当前状态
     */
    private State requireState() {
        State state = current.get();
        if (state == null) {
            throw new IllegalStateException("工具调用未绑定 Agent 会话");
        }
        return state;
    }

    /**
     * 返回给编排器的调用快照。
     *
     * @param sessionId 会话标识
     * @param toolCallCount 实际工具数
     * @param citations 可信引用
     * @param draft 最后草案
     */
    public record Snapshot(String sessionId, int toolCallCount, List<KnowledgeCitationVO> citations,
                           ActionDraftVO draft) {
    }

    /** 线程内可变调用状态。 */
    private static final class State {
        /** 当前会话。 */
        private final String sessionId;
        /** 已执行工具数。 */
        private int toolCallCount;
        /** 以 chunkId 去重的可信引用。 */
        private final Map<String, KnowledgeCitationVO> citations = new LinkedHashMap<>();
        /** 本轮最新草案。 */
        private ActionDraftVO draft;

        /**
         * 创建调用状态。
         *
         * @param sessionId 当前会话
         */
        private State(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
