package com.example.crispixai.service.impl;

import com.example.crispixai.model.ChatEvent;
import com.example.crispixai.service.AgentService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agent 对话服务实现
 * 封装 HarnessAgent 的同步对话和流式对话能力
 */
@Slf4j
@Service
public class AgentServiceImpl implements AgentService {

    @Resource
    private HarnessAgent harnessAgent;

    @Override
    public Mono<Msg> chat(String userId, String sessionId, String message) {
        log.info("同步对话请求 - userId: {}, sessionId: {}, message: {}", userId, sessionId, message);

        RuntimeContext context = RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        return harnessAgent.call(new UserMessage(message), context)
                .doOnSuccess(msg -> log.info("同步对话完成 - userId: {}, sessionId: {}", userId, sessionId))
                .doOnError(error -> log.error("同步对话失败 - userId: {}, sessionId: {}", userId, sessionId, error));
    }

    @Override
    public Flux<ChatEvent> chatStream(String userId, String sessionId, String message) {
        log.info("流式对话请求 - userId: {}, sessionId: {}, message: {}", userId, sessionId, message);
        // 获取历史上下文
        RuntimeContext context = RuntimeContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        // 流式调用llm
        return harnessAgent.streamEvents(new UserMessage(message), context)
                .map(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        TextBlockDeltaEvent textEvent = (TextBlockDeltaEvent) event;
                        return ChatEvent.textDelta(textEvent.getDelta());
                    } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                        return new ChatEvent("tool_call", "工具调用开始");
                    } else if (event.getType() == AgentEventType.THINKING_BLOCK_DELTA) {
                        ThinkingBlockDeltaEvent thinkingEvent = (ThinkingBlockDeltaEvent) event;
                        return new ChatEvent("thinking", thinkingEvent.getDelta());
                    } else {
                        return ChatEvent.textDelta("");
                    }
                })
                .concatWith(Flux.just(ChatEvent.done()))
                .doOnComplete(() -> log.info("流式对话完成 - userId: {}, sessionId: {}", userId, sessionId))
                .doOnError(error -> log.error("流式对话失败 - userId: {}, sessionId: {}", userId, sessionId, error));
    }

    @Override
    public String getConversationHistory(String userId, String sessionId) {
        log.info("获取对话历史 - userId: {}, sessionId: {}", userId, sessionId);
        return "对话历史功能待实现";
    }
}