package io.github.decadedx.springaiagent.controller;

import io.github.decadedx.springaiagent.common.Result;
import io.github.decadedx.springaiagent.dto.ChatMessageDTO;
import io.github.decadedx.springaiagent.dto.ChatSessionCreateDTO;
import io.github.decadedx.springaiagent.service.AgentOrchestrationService;
import io.github.decadedx.springaiagent.service.ChatSessionService;
import io.github.decadedx.springaiagent.service.ChatRateLimiter;
import io.github.decadedx.springaiagent.vo.ChatMessageVO;
import io.github.decadedx.springaiagent.vo.ChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供用户隔离的 Agent 会话创建与消息接口。
 */
@RestController
@RequestMapping("/api/chat/sessions")
public class ChatController {

    /** Redis 会话服务。 */
    private final ChatSessionService chatSessionService;

    /** Agent 编排服务。 */
    private final AgentOrchestrationService agentOrchestrationService;

    /** 聊天限流服务。 */
    private final ChatRateLimiter chatRateLimiter;

    /**
     * 创建聊天控制器。
     *
     * @param chatSessionService 会话服务
     * @param agentOrchestrationService Agent 编排服务
     */
    public ChatController(ChatSessionService chatSessionService, AgentOrchestrationService agentOrchestrationService,
                          ChatRateLimiter chatRateLimiter) {
        this.chatSessionService = chatSessionService;
        this.agentOrchestrationService = agentOrchestrationService;
        this.chatRateLimiter = chatRateLimiter;
    }

    /**
     * 为当前认证用户创建短期聊天会话。
     *
     * @param createDTO 可选会话名称
     * @return 会话摘要
     */
    @PostMapping
    public ResponseEntity<Result<ChatSessionVO>> create(@Valid @RequestBody(required = false) ChatSessionCreateDTO createDTO) {
        ChatSessionVO session = chatSessionService.create(createDTO == null ? null : createDTO.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(session));
    }

    /**
     * 向当前用户自己的会话发送一条消息。
     *
     * @param sessionId 会话标识
     * @param messageDTO 用户消息
     * @return Agent 响应
     */
    @PostMapping("/{sessionId}/messages")
    public Result<ChatMessageVO> send(@PathVariable String sessionId, @Valid @RequestBody ChatMessageDTO messageDTO) {
        chatRateLimiter.check();
        return Result.success(agentOrchestrationService.send(sessionId, messageDTO));
    }
}
