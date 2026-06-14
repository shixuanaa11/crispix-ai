package com.example.crispixai.controller;

import com.example.crispixai.common.BaseResponse;
import com.example.crispixai.common.ErrorCode;
import com.example.crispixai.common.ResultUtils;
import com.example.crispixai.model.ChatEvent;
import com.example.crispixai.model.ChatRequest;
import com.example.crispixai.model.ChatResponse;
import com.example.crispixai.service.AgentService;
import io.agentscope.core.message.Msg;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Agent 对话控制器
 * 提供同步对话和流式对话接口
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent 对话", description = "AI Agent 对话接口")
public class AgentController {

    private final AgentService agentService;

    /**
     * 同步对话接口
     */
    @PostMapping("/chat")
    @Operation(summary = "同步对话", description = "发送消息并等待完整回复")
    public BaseResponse<ChatResponse> chat(
            @Parameter(description = "用户 ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "会话 ID", required = false)
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestBody ChatRequest request) {

        log.info("收到同步对话请求 - userId: {}, sessionId: {}", userId, sessionId);

        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        String effectiveUserId = userId != null ? userId : "anonymous";
        String effectiveSessionId = sessionId != null ? sessionId : "default-session";

        try {
            Msg response = agentService.chat(effectiveUserId, effectiveSessionId, request.getMessage()).block();

            if (response == null) {
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "Agent 响应为空");
            }

            // AgentScope 2.0 的 Msg.getContent() 返回 List<ContentBlock>
            // 提取文本内容拼接为字符串
            String contentText = response.getContent().stream()
                    .map(Object::toString)
                    .reduce("", (a, b) -> a + b);

            ChatResponse chatResponse = ChatResponse.builder()
                    .sessionId(effectiveSessionId)
                    .userId(effectiveUserId)
                    .content(contentText)
                    .model(request.getModel() != null ? request.getModel() : "default")
                    .build();

            return ResultUtils.success(chatResponse);

        } catch (Exception e) {
            log.error("同步对话失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "对话失败: " + e.getMessage());
        }
    }

    /**
     * 流式对话接口（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "发送消息并以 SSE 流式返回响应")
    public Flux<ChatEvent> chatStream(
            @Parameter(description = "用户 ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "会话 ID", required = false)
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestBody ChatRequest request) {

        log.info("收到流式对话请求 - userId: {}, sessionId: {}", userId, sessionId);

        String effectiveUserId = userId != null ? userId : "anonymous";
        String effectiveSessionId = sessionId != null ? sessionId : "default-session";

        try {
            return agentService.chatStream(effectiveUserId, effectiveSessionId, request.getMessage());
        } catch (Exception e) {
            log.error("流式对话失败", e);
            return Flux.just(ChatEvent.error("对话失败: " + e.getMessage()));
        }
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取对话历史", description = "获取指定会话的对话历史记录")
    public BaseResponse<String> getHistory(
            @Parameter(description = "用户 ID", required = true)
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "会话 ID", required = true)
            @RequestParam("sessionId") String sessionId) {

        log.info("获取对话历史 - userId: {}, sessionId: {}", userId, sessionId);

        String effectiveUserId = userId != null ? userId : "anonymous";

        try {
            String history = agentService.getConversationHistory(effectiveUserId, sessionId);
            return ResultUtils.success(history);
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "获取历史失败: " + e.getMessage());
        }
    }
}
